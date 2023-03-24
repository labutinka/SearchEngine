package searchengine.exeptions;

public class ApiException extends RuntimeException{
    public ApiException(String word){
        super(word);
    }
}
