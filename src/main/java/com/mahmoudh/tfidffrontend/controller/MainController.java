package com.mahmoudh.tfidffrontend.controller;

import com.mahmoudh.tfidffrontend.model.Results;
import com.mahmoudh.tfidffrontend.model.SerializationUtil;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@RestController
public class MainController implements Watcher {
    final static public String ZOOKEEPER_ADDRESS="localhost:2181";
    private static final int TIME_OUT = 5000;
    ZooKeeper zooKeeper;
    HttpClient client;


    public MainController() throws IOException {

    }
    @GetMapping("/search")
    ResponseEntity<Map<String, Double>> getResult(@RequestParam String terms) throws InterruptedException, KeeperException, ExecutionException, IOException {
        CompletableFuture<Map<String,Double>> results;
        zooKeeper=new ZooKeeper(ZOOKEEPER_ADDRESS,TIME_OUT,this);
        this.client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

        List<String> child=zooKeeper.getChildren("/co-service_registry",false);
        Stat stat=zooKeeper.exists("/co-service_registry/"+child.get(0),false);
        byte[] data=zooKeeper.getData("/co-service_registry/"+child.get(0),false,stat);
        String leaderEndPoint= new String(data);

        HttpRequest httpRequest= HttpRequest.newBuilder()
                .POST(HttpRequest.BodyPublishers.ofByteArray(SerializationUtil.serialize(terms)))
                .uri(URI.create(leaderEndPoint))
                .build();
        results= client.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofByteArray())
                .thenApply(HttpResponse::body)
                .thenApply(res -> (Map<String, Double>) SerializationUtil.deserialize(res));

        return new ResponseEntity<Map<String, Double>>(results.get(), HttpStatus.OK);

    }

    @Override
    public void process(WatchedEvent watchedEvent) {

    }
}

