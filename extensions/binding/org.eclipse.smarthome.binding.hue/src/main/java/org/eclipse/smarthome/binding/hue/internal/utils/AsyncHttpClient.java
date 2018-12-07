/**
 * Copyright (c) 2014,2018 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.eclipse.smarthome.binding.hue.internal.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.HttpResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.BufferingResponseListener;
import org.eclipse.jetty.client.util.InputStreamContentProvider;
import org.eclipse.jetty.http.HttpMethod;;

/**
 * An asynchronous API for HTTP interactions.
 *
 * @author David Graeff - Initial contribution
 */
@NonNullByDefault
public class AsyncHttpClient {
    private final HttpClient client;
    private int timeout;

    public AsyncHttpClient(HttpClient client, int timeout) {
        this.client = client;
        this.timeout = timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public Result get(String address) throws IOException {
        try {
            return doNetwork(HttpMethod.GET, address, null, timeout).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException(e);
        }
    }

    public Result post(String address, String jsonString) throws IOException {
        try {
            return doNetwork(HttpMethod.POST, address, jsonString, timeout).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException(e);
        }
    }

    public Result put(String address, String jsonString) throws IOException {
        try {
            return doNetwork(HttpMethod.PUT, address, jsonString, timeout).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException(e);
        }
    }

    public Result delete(String address) throws IOException {
        try {
            return doNetwork(HttpMethod.DELETE, address, null, timeout).get();
        } catch (InterruptedException | ExecutionException e) {
            throw new IOException(e);
        }
    }

    /**
     * Perform a POST request
     *
     * @param address The address
     * @param jsonString The message body
     * @param timeout A timeout
     * @return The result
     * @throws IOException Any IO exception in an error case.
     */
    public CompletableFuture<Result> post(String address, String jsonString, int timeout) {
        return doNetwork(HttpMethod.POST, address, jsonString, timeout);
    }

    /**
     * Perform a PUT request
     *
     * @param address The address
     * @param jsonString The message body
     * @param timeout A timeout
     * @return The result
     * @throws IOException Any IO exception in an error case.
     */
    public CompletableFuture<Result> put(String address, String jsonString, int timeout) {
        return doNetwork(HttpMethod.PUT, address, jsonString, timeout);
    }

    /**
     * Perform a GET request
     *
     * @param address The address
     * @param timeout A timeout
     * @return The result
     * @throws IOException Any IO exception in an error case.
     */
    public CompletableFuture<Result> get(String address, int timeout) {
        return doNetwork(HttpMethod.GET, address, null, timeout);
    }

    /**
     * Perform a DELETE request
     *
     * @param address The address
     * @param timeout A timeout
     * @return The result
     * @throws IOException Any IO exception in an error case.
     */
    public CompletableFuture<Result> delete(String address, int timeout) {
        return doNetwork(HttpMethod.DELETE, address, null, timeout);
    }

    private CompletableFuture<Result> doNetwork(HttpMethod method, String address, @Nullable String body, int timeout) {
        final CompletableFuture<Result> f = new CompletableFuture<Result>();
        Request request = client.newRequest(URI.create(address));
        if (body != null) {
            try (ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(body.getBytes());
                    final InputStreamContentProvider inputStreamContentProvider = new InputStreamContentProvider(
                            byteArrayInputStream)) {
                request.content(inputStreamContentProvider, "application/json");
            } catch (Exception e) {
                f.completeExceptionally(e);
                return f;
            }
        }

        request.method(method).timeout(timeout, TimeUnit.MILLISECONDS).send(new BufferingResponseListener() {
            @NonNullByDefault({})
            @Override
            public void onComplete(org.eclipse.jetty.client.api.Result result) {
                final HttpResponse response = (HttpResponse) result.getResponse();
                if (result.getFailure() != null) {
                    f.completeExceptionally(result.getFailure());
                    return;
                }
                f.complete(new Result(getContentAsString(), response.getStatus(), address));
            }
        });
        return f;
    }

    public static class Result {
        private final String body;
        private final int responseCode;
        private final String requestURL;

        public Result(String body, int responseCode, String requestURL) {
            this.body = body;
            this.responseCode = responseCode;
            this.requestURL = requestURL;
        }

        public String getBody() {
            return body;
        }

        public int getResponseCode() {
            return responseCode;
        }

        public String getRequestURL() {
            return requestURL;
        }
    }
}
