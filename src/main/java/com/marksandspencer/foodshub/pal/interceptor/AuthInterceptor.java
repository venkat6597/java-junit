package com.marksandspencer.foodshub.pal.interceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import com.marksandspencer.assemblyservice.config.utils.ApplicationConstants;
import com.marksandspencer.foodshub.pal.constant.ApplicationConstant;
import com.marksandspencer.foodshub.pal.constant.ErrorCode;
import com.marksandspencer.foodshub.pal.exception.PALServiceException;
import com.marksandspencer.foodshub.pal.service.UserDetailsService;
import com.marksandspencer.foodshub.pal.transfer.UserDetails;

/**
 * AuthInterceptor
 *
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

	@Autowired
	private UserDetailsService userDetailsService;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
			throws Exception {
		if (ApplicationConstants.OPTIONS.equalsIgnoreCase(request.getMethod())) {
			return true;
		}
		UserDetails userDetails = userDetailsService.getUserDetails();
		if (!ObjectUtils.isEmpty(userDetails)
				&& ApplicationConstant.SUPPLIER_ROLE_NAME.equals(userDetails.getUserRole().getRoleName())) {
			throw new PALServiceException(ErrorCode.FORBIDEEN);
		}
		return HandlerInterceptor.super.preHandle(request, response, handler);
	}
}
