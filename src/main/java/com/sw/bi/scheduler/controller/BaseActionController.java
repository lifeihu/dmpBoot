package com.sw.bi.scheduler.controller;

import com.sw.bi.scheduler.model.AuthenticationUserGroup;
import com.sw.bi.scheduler.service.GenericService;
import com.sw.bi.scheduler.util.BeanUtil;
import com.sw.bi.scheduler.util.OperateAction;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.AuthenticationUserDetails;
import org.springframework.ui.ConditionModel;
import org.springframework.ui.Model;
import org.springframework.ui.PaginationSupport;
import org.springframework.util.JsonUtil;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.ParameterizedType;
import java.util.*;

@SuppressWarnings("unchecked")
public abstract class BaseActionController<T> {
	protected static final Logger log = Logger.getLogger(BaseActionController.class);

	private Class<T> entityClass;
	private String entityClassName;

	protected HttpServletRequest request;
	protected HttpServletResponse response;

	public BaseActionController() {
		entityClass = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
		entityClassName = BeanUtil.convertPropertyName(entityClass.getSimpleName());
	}

	@ModelAttribute
	public void prepared(HttpServletRequest request, HttpServletResponse response) {
		this.request = request;
		this.response = response;
	}

	@RequestMapping("")
	@ResponseBody
	public Map execute(Long id) {
		Map map = new HashMap();
		Object entity = null;

		if (id != null)
			entity = getDefaultService().get(id);
		map.put(entityClassName, entity);
		return map;
	}

	@RequestMapping("list")
	@ResponseBody
	public List<T> list(@RequestParam("condition")
                                ConditionModel cm, @RequestParam(required = false)
	String sort, @RequestParam(value = "dir", required = false)
	String direction) {
		cm.addOrder(sort, direction);
		return getDefaultService().query(cm);
	}

	@RequestMapping("paging")
	@ResponseBody
	public PaginationSupport paging(@RequestParam("condition")
                                            ConditionModel cm, Integer start, Integer limit, @RequestParam(required = false)
	String sort, @RequestParam(value = "dir", required = false)
	String direction) {
		cm.setStart(start);
		cm.setLimit(limit);
//		cm.addOrder(sort, direction);

		return getDefaultService().paging(cm, Criteria.ROOT_ENTITY);
	}

	@RequestMapping("save")
	@ResponseBody
	public T save(String[] childrenDataRoot) {
		T entity = (T) decode(entityClassName, entityClass);

		if (entity == null)
			return null;

		if (entity instanceof AuthenticationUserGroup) {
			getDefaultService().isAuthorizedUserGroup((AuthenticationUserGroup) entity, getDefaultService().getEntityIdValue(entity) == null ? OperateAction.CREATE : OperateAction.UPDATE);
		}

		if (childrenDataRoot != null) {
			for (String dataRoot : childrenDataRoot) {
				try {
					// 创建与主表的关系
					Collection<Object> children = (Collection<Object>) PropertyUtils.getProperty(entity, dataRoot);
					for (Object child : children) {
						if (PropertyUtils.isWriteable(child, entityClassName)) {
							PropertyUtils.setProperty(child, entityClassName, entity);

						} else if (PropertyUtils.isReadable(child, entityClassName + "s")) {
							((Collection<T>) PropertyUtils.getProperty(child, entityClassName + "s")).add(entity);

						}
					}

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		getDefaultService().saveOrUpdate(entity);

		return entity;
	}

	@RequestMapping("remove")
	@ResponseBody
	public void remove(String id) {
		if (!StringUtils.hasText(id)) {
			return;
		}

		Collection<T> entities = new ArrayList<T>();
		for (String entityId : id.split(",")) {
			T entity = getDefaultService().get(Long.valueOf(entityId));

			if (entity instanceof AuthenticationUserGroup) {
				getDefaultService().isAuthorizedUserGroup((AuthenticationUserGroup) entity, OperateAction.DELETE);
			}

			entities.add(entity);
		}

		getDefaultService().delete(entities);
	}

	///////////////////////////////////////////////////////////

	@SuppressWarnings("hiding")
	protected <T> T decode(String paramName, Class<?> clazz) {
		return (T) JsonUtil.decode(request.getParameter(paramName), clazz);
	}

	@SuppressWarnings("hiding")
	protected <T> T decodeCollection(String paramName, Class<?> elementClazz) {
		return JsonUtil.decode(request.getParameter(paramName), List.class, elementClazz);
	}

	protected String encode(Object entity) {
		return JsonUtil.encode(entity);
	}

	///////////////////////////////////////////////////////////

	/**
	 * 获得登录用户
	 * 
	 * @return
	 */
	protected AuthenticationUserDetails getPrincipal() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null) {
			// throw new SessionTimeoutException("用户未登录.");
			return null;
		}

		Object principal = authentication.getPrincipal();
		if (principal instanceof String) {
			// throw new SessionTimeoutException("用户未登录.");
			return null;
		}

		return (AuthenticationUserDetails) principal;
	}

	///////////////////////////////////////////////////////////

	public abstract GenericService<T> getDefaultService();
}
