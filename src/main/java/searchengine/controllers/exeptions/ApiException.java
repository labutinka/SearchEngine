package searchengine.controllers.exeptions;

public class ApiException extends Exception{
    public ApiException(String message){
        super(message);
    }
}
