package fr.insee.metallica.outbox.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;

import fr.insee.metallica.outbox.OutboxCommandService;

@Aspect
@Component
public class TransactionalCommandAspect implements Ordered {
	@Autowired
	private OutboxCommandService outboxCommandService;

	@SuppressWarnings("unchecked")
	@Around("@annotation(TransactionalCommand) &&  @annotation(transactionalCommand)")
	public Object commandTransaction(ProceedingJoinPoint joinPoint, TransactionalCommand transactionalCommand) throws Throwable {
		return outboxCommandService.execute((status) -> {
			try {
				return joinPoint.proceed();
			} catch (Throwable e) {
				throw new RuntimeException("could not execute method");
			}
		}, ((MethodSignature)joinPoint.getSignature()).getReturnType()).get();
	}

	@Override
	public int getOrder() {
		return Ordered.LOWEST_PRECEDENCE;
	}
}
