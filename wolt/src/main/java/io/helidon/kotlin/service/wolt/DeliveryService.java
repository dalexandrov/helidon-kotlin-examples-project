/*
 * Copyright (c) 2019, 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.helidon.kotlin.service.wolt;

import io.helidon.common.http.Http;
import io.helidon.common.reactive.Multi;
import io.helidon.common.reactive.Single;
import io.helidon.dbclient.DbClient;
import io.helidon.dbclient.DbRow;
import io.helidon.webserver.*;

import javax.json.JsonObject;
import java.util.concurrent.CompletionException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Common methods that do not differ between JDBC and MongoDB.
 */
public abstract class DeliveryService implements Service {

    private static final Logger LOGGER = Logger.getLogger(DeliveryService.class.getName());

    private final DbClient dbClient;


    protected DeliveryService(DbClient dbClient) {
        this.dbClient = dbClient;
        // MySQL init
        dbClient.execute(handle -> handle.namedDml("create-table"))
                .thenAccept(System.out::println)
                .exceptionally(throwable -> {
                    LOGGER.log(Level.WARNING, "Failed to create table, maybe it already exists?", throwable);
                    return null;
                });
    }

    @Override
    public void update(Routing.Rules rules) {
        rules
                .get("/", this::listDeliveries)
                // create new
                .put("/", Handler.create(Delivery.class, this::insertDelivery))
                // update existing
                .post("/{id}/{food}/{address}/{status}", this::insertDeliverySimple)
                // delete all
                .delete("/", this::deleteAllDeliveries)
                // get one
                .get("/{address}", this::getDelivery)
                // delete one
                .delete("/{id}", this::deleteDelivery)
                // example of transactional API (local transaction only!)
                .put("/transactional", Handler.create(Delivery.class, this::transactional))
                // update one (TODO this is intentionally wrong - should use JSON request, just to make it simple we use path)
                .put("/{id}/{food}/{address}/{status}", this::updateDelivery);
    }


    protected DbClient dbClient() {
        return dbClient;
    }

    private void deleteAllDeliveries(ServerRequest request, ServerResponse response){
        dbClient().execute(exec -> exec
                        // this is to show how ad-hoc statements can be executed (and their naming in Tracing and Metrics)
                        .createDelete("DELETE FROM deliveries")
                        .execute())
                .thenAccept(count -> response.send("Deleted: " + count + " values"))
                .exceptionally(throwable -> sendError(throwable, response));
    }


    private void insertDelivery(ServerRequest request, ServerResponse response, Delivery delivery) {
        dbClient.execute(exec -> exec
                .createNamedInsert("insert2")
                .namedParam(delivery)
                .execute())
                .thenAccept(count -> response.send("Inserted: " + count + " values"))
                .exceptionally(throwable -> sendError(throwable, response));
    }


    private void insertDeliverySimple(ServerRequest request, ServerResponse response) {

        Delivery delivery = new Delivery(request.path().param("id"),
                request.path().param("food"),
                request.path().param("address"),
                DeliveryStatus.valueOf(request.path().param("status")));

        dbClient.execute(exec -> exec
                .createNamedInsert("insert2")
                .namedParam(delivery)
                .execute())
                .thenAccept(count -> response.send("Inserted: " + count + " values"))
                .exceptionally(throwable -> sendError(throwable, response));
    }


    private void getDelivery(ServerRequest request, ServerResponse response) {
        String delivery = request.path().param("address");

        dbClient.execute(exec -> exec.namedGet("select-one", delivery))
                .thenAccept(opt -> opt.ifPresentOrElse(it -> sendRow(it, response),
                                                       () -> sendNotFound(response, "Delivery to "
                                                               + delivery
                                                               + " not found")))
                .exceptionally(throwable -> sendError(throwable, response));
    }


    private void listDeliveries(ServerRequest request, ServerResponse response) {
        Multi<JsonObject> rows = dbClient.execute(exec -> exec.namedQuery("select-all"))
                .map(it -> it.as(JsonObject.class));

        response.send(rows, JsonObject.class);
    }


    private void updateDelivery(ServerRequest request, ServerResponse response) {
        final String id = request.path().param("id");
        final String food = request.path().param("food");
        final String address = request.path().param("address");
        final String status = request.path().param("address");

        dbClient.execute(exec -> exec
                .createNamedUpdate("update")
                .addParam("id", id)
                .addParam("food", food)
                .addParam("address", address)
                .addParam("status", status)
                .execute())
                .thenAccept(count -> response.send("Updated: " + count + " values"))
                .exceptionally(throwable -> sendError(throwable, response));
    }

    private void transactional(ServerRequest request, ServerResponse response, Delivery delivery) {

        dbClient.inTransaction(tx -> tx
                .createNamedGet("select-for-update")
                .namedParam(delivery)
                .execute()
                .flatMapSingle(maybeRow -> maybeRow.map(dbRow -> tx.createNamedUpdate("update")
                        .namedParam(delivery).execute())
                        .orElseGet(() -> Single.just(0L)))
        ).thenAccept(count -> response.send("Updated " + count + " records"));

    }


    private void deleteDelivery(ServerRequest request, ServerResponse response) {
        final String id = request.path().param("id");

        dbClient.execute(exec -> exec.namedDelete("delete", id))
                .thenAccept(count -> response.send("Deleted: " + count + " values"))
                .exceptionally(throwable -> sendError(throwable, response));
    }


    protected void sendNotFound(ServerResponse response, String message) {
        response.status(Http.Status.NOT_FOUND_404);
        response.send(message);
    }


    protected void sendRow(DbRow row, ServerResponse response) {
        response.send(row.as(JsonObject.class));
    }


    protected <T> T sendError(Throwable throwable, ServerResponse response) {
        Throwable realCause = throwable;
        if (throwable instanceof CompletionException) {
            realCause = throwable.getCause();
        }
        response.status(Http.Status.INTERNAL_SERVER_ERROR_500);
        response.send("Failed to process request: " + realCause.getClass().getName() + "(" + realCause.getMessage() + ")");
        LOGGER.log(Level.WARNING, "Failed to process request", throwable);
        return null;
    }

}
