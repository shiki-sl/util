package com.shiki.util.aop;

import cn.hutool.core.exceptions.ExceptionUtil;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;

import static java.lang.System.currentTimeMillis;

/**
 * @author shiki
 * @description 基于Aspect的controller层日志拦截，解决get请求体无法打印日志的情况下
 * @date 2019/8/21 16:18
 */
@Aspect
@Slf4j
@Component
public class LogAop {

	//	切入点表达式 匹配controller层中所有的方法,排除上传文件
	@Pointcut("execution(* com.*.*.other.controller.*.*(..)) " +
		"&& !execution(* com.*.*.other.controller.*.*upload*(..))")
	public void log() {
	}

	@Around("log()")
	public Object logHandler(ProceedingJoinPoint joinPoint) {
		ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
		HttpServletRequest req = attributes.getRequest();
		StringBuffer url = req.getRequestURL();
		log.info("请求url:{}", url);
		String method = req.getMethod();
		log.info("HTTP_METHOD:{}", method);
		log.info("IP:{}", req.getRemoteAddr());

		Signature signature = joinPoint.getSignature();
//		aop代理类的名字
		String classPath = signature.getDeclaringTypeName();
		log.info("类:{}", classPath);
		String methodName = signature.getName();
		log.info("方法:{}", methodName);
		String suffix = classPath.replace("com.gcyl.pig.", "");
		log.info("模块:{}", suffix.substring(0, suffix.indexOf(".")));
		MethodSignature methodSignature = (MethodSignature) signature;
// 入参
		StringBuilder params = new StringBuilder();
//		上传文件不打印参数信息
		// 获取方法参数名称
		String[] paramNames = methodSignature.getParameterNames();
		Object[] paramValues = joinPoint.getArgs();

		int length = paramNames.length;
		for (int i = 0; i < length; i++) {
			params.append(paramNames[i]).append("=").append(JSON.toJSONString(paramValues[i])).append("</br>").append("\r\n");
		}
		log.info("参数列表:{}", params);

		long costTime = currentTimeMillis();
		Object result = null;
		try {
			// 执行controller方法
			result = joinPoint.proceed();
		} catch (Throwable throwable) {
			String stackTrace = ExceptionUtil.stacktraceToString(throwable);
			String exception = throwable.getClass() + ":" + throwable.getMessage();
			log.error("方法异常throwable:{}", stackTrace);
			log.error("方法异常exception:{}", exception);
		} finally {
			costTime = currentTimeMillis() - costTime;
			log.info(classPath + "." + methodName + "方法执行完毕,请求url为{},耗时:{}ms", url, costTime);
		}
		// 存入数据库或日志文件
		return result;
	}

}
