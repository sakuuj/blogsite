package by.sakuuj.blogsite.article.exception;

public class IdempotencyTokenExistsException extends RuntimeException {

    public IdempotencyTokenExistsException() {
    }

    public IdempotencyTokenExistsException(String message) {
        super(message);
    }
}
