package com.omfgdevelop.ntmes;

import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
public class NtController {
    @Value("${token}")
    String token;
    final int count = 200;
    final int threads = 2000;

    long assignmentId = 482921;

    String requestPath = "/historyNotProgramming";

    @GetMapping
    public ResponseEntity<ResponseDto> start() throws ExecutionException, InterruptedException {

        ResponseDto responseDto = new ResponseDto();
        System.out.println("Success");
        List<FutureTask<ResponseDto>> list = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            var callable = threadStarter(assignmentId - ((i + 1) * count));
            FutureTask<ResponseDto> future = new FutureTask<>(callable);
            new Thread(future).start();
            list.add(future);
        }

        for (FutureTask<ResponseDto> responseDtoFutureTask : list) {
            ResponseDto resp = responseDtoFutureTask.get();
            log.atInfo().setMessage("Thread is completed").log();
            responseDto.setSuccessCount(resp.getSuccessCount() + responseDto.getSuccessCount());
            responseDto.setErrorCount(resp.getErrorCount() + responseDto.getErrorCount());
        }

        return ResponseEntity.ok(responseDto);
    }

    private Callable<ResponseDto> threadStarter(long param) {
        return () -> {
            ResponseDto responseDto = new ResponseDto();
            for (int i = 0; i < count; i++) {
                var call = new CallBack() {
                    @Override
                    public void callBackSuccess(String response) {
                        responseDto.setSuccessCount(responseDto.getSuccessCount() + 1);
                    }

                    @Override
                    public void callbackFailed(String error) {
                        responseDto.setErrorCount(responseDto.getErrorCount() + 1);
                    }
                };
                request(call, i + param);
            }

            return responseDto;
        };

    }

    private void request(CallBack callBack, long param) {

        final RestTemplate restTemplate = new RestTemplate();
        HttpHeaders httpHeaders = new HttpHeaders();
        MultiValueMap<String, String> params = new LinkedMultiValueMap<String, String>();
        params.add("Authorization", "Bearer " + token);
        httpHeaders.setContentType(MediaType.TEXT_PLAIN);
        httpHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        httpHeaders.set("Authorization", "Bearer " + token);

        HttpEntity<String> entity = new HttpEntity<String>("null", httpHeaders);

        try {
            ResponseEntity<String> response = restTemplate.exchange("http://localhost:8080/api/task-attempt/" + param + requestPath, HttpMethod.GET, entity, String.class);
            log.atInfo().setMessage("Success").addKeyValue("body", response.getBody()).log();
            callBack.callBackSuccess(response.getBody());
        } catch (Exception e) {
            log.atError().setMessage("error").setCause(e).log();
            callBack.callbackFailed(e.getMessage());
        }
    }

    interface CallBack {
        void callBackSuccess(String response);

        void callbackFailed(String error);
    }

    @Getter
    @Setter
    class ResponseDto {
        private int successCount = 0;

        private int errorCount = 0;
    }
}
