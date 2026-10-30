package web.graphql;

import domain.AccountServiceUnavailableException;
import domain.InvalidTransactionException;
import domain.TransactionNotFoundException;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;

@Component
public class TransactionGraphqlExceptionHandler extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable exception, DataFetchingEnvironment env) {
        if (exception instanceof TransactionNotFoundException) {
            return GraphqlErrorBuilder.newError(env)
                    .errorType(ErrorType.NOT_FOUND)
                    .message(exception.getMessage())
                    .build();
        }
        if (exception instanceof InvalidTransactionException) {
            return GraphqlErrorBuilder.newError(env)
                    .errorType(ErrorType.BAD_REQUEST)
                    .message(exception.getMessage())
                    .build();
        }
        if (exception instanceof AccountServiceUnavailableException) {
            return GraphqlErrorBuilder.newError(env)
                    .errorType(ErrorType.INTERNAL_ERROR)
                    .message(exception.getMessage())
                    .build();
        }
        return null;
    }
}
