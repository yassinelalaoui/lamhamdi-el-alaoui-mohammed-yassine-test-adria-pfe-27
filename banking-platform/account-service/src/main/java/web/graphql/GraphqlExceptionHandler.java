package web.graphql;

import domain.AccountBlockedException;
import domain.AccountNotFoundException;
import domain.BalanceOperationConflictException;
import domain.InsufficientFundsException;
import domain.InvalidAccountCommandException;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import org.springframework.graphql.data.method.annotation.GraphQlExceptionHandler;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Controller;

@Controller
public class GraphqlExceptionHandler {
    @GraphQlExceptionHandler
    public GraphQLError handleNotFound(AccountNotFoundException exception) {
        return error(ErrorType.NOT_FOUND, exception.getMessage());
    }

    @GraphQlExceptionHandler
    public GraphQLError handleBlocked(AccountBlockedException exception) {
        return error(ErrorType.BAD_REQUEST, exception.getMessage());
    }

    @GraphQlExceptionHandler
    public GraphQLError handleInsufficientFunds(InsufficientFundsException exception) {
        return error(ErrorType.BAD_REQUEST, exception.getMessage());
    }

    @GraphQlExceptionHandler
    public GraphQLError handleOperationConflict(BalanceOperationConflictException exception) {
        return error(ErrorType.BAD_REQUEST, exception.getMessage());
    }

    @GraphQlExceptionHandler
    public GraphQLError handleInvalidCommand(InvalidAccountCommandException exception) {
        return error(ErrorType.BAD_REQUEST, exception.getMessage());
    }

    private GraphQLError error(ErrorType type, String message) {
        return GraphqlErrorBuilder.newError().errorType(type).message(message).build();
    }
}