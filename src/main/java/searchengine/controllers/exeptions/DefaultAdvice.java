package searchengine.controllers.exeptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import searchengine.dto.FalseResponse;

@ControllerAdvice
public class DefaultAdvice {
    @ExceptionHandler
    public ResponseEntity<FalseResponse> handleException(ApiException e) {
        FalseResponse response = new FalseResponse(e.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
}
