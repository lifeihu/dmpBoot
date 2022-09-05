package org.springframework.resolver;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.handler.AbstractHandlerExceptionResolver;

public class ExceptionResolver extends AbstractHandlerExceptionResolver {
	private static final Logger log = Logger.getLogger(ExceptionResolver.class);

	@Override
	protected ModelAndView doResolveException(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
		boolean isAjax = "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
		ModelAndView result = null;

		if (isAjax) {
			boolean isWarning = ex instanceof Warning;

			result = new ModelAndView();
			result.addObject("success", false);
			result.addObject("errorType", isWarning ? "warning" : "error");

			if (!isWarning) {
				StringWriter sw = new StringWriter();
				PrintWriter pw = new PrintWriter(sw);
				ex.printStackTrace(pw);
				result.addObject("stackTrace", formatEnter(sw.getBuffer().toString()));

				try {
					pw.close();
					sw.close();
				} catch (Exception e) {
					e.printStackTrace();
				}

				ex.printStackTrace();
			} else {
				log.error(ex.getMessage());
			}

			result.addObject("msg", this.getMessage(ex));
		}

		return result;
	}

	private String getMessage(Exception e) {
		return e.getMessage() == null ? "操作失败!" : formatEnter(e.getMessage());
	}

	private String formatEnter(String message) {
		if (StringUtils.hasText(message)) {
			return message.replaceAll("\\\r\\\n", "\\\\\\r\\\\\\n");
		}

		return "";
	}
}
