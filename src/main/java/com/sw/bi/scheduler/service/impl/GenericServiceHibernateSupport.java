package com.sw.bi.scheduler.service.impl;

import com.sw.bi.scheduler.model.AuthenticationUserGroup;
import com.sw.bi.scheduler.model.LoggerEntity;
import com.sw.bi.scheduler.model.User;
import com.sw.bi.scheduler.model.UserGroup;
import com.sw.bi.scheduler.service.GenericService;
import com.sw.bi.scheduler.service.OperateLoggerService;
import com.sw.bi.scheduler.service.UserGroupRelationService;
import com.sw.bi.scheduler.service.UserService;
import com.sw.bi.scheduler.util.BeanUtil;
import com.sw.bi.scheduler.util.OperateAction;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.beanutils.converters.DateConverter;
import org.apache.log4j.Logger;
import org.hibernate.*;
import org.hibernate.criterion.*;
import org.hibernate.transform.ResultTransformer;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.orm.hibernate5.HibernateCallback;
import org.springframework.orm.hibernate5.support.HibernateDaoSupport;
import org.springframework.resolver.Warning;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.AuthenticationUserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ConditionExpression;
import org.springframework.ui.ConditionModel;
import org.springframework.ui.OrderBy;
import org.springframework.ui.PaginationSupport;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import javax.persistence.EntityManagerFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

@SuppressWarnings("unchecked")
public class GenericServiceHibernateSupport<T> extends HibernateDaoSupport implements GenericService<T>, ApplicationContextAware {
	protected static final Logger log = Logger.getLogger(GenericServiceHibernateSupport.class);

	@Autowired
	private EntityManagerFactory entityManagerFactory;

	private ApplicationContext context;
	protected Class<T> entityClass;

	public GenericServiceHibernateSupport() {
		DateConverter dtc = new DateConverter();
		dtc.setPatterns(new String[] { "yyyy-M-d", "yyyy-M-d HH:mm:ss", "yyyy-MM-dd", "yyyy-MM-dd HH:mm:ss" });
		ConvertUtils.register(dtc, Date.class);

		entityClass = (Class<T>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
	}

	@Resource(name = "schedulerSessionFactory")
	public void setSessionFactory1(SessionFactory sessionFactory) {
		super.setSessionFactory(sessionFactory);
	}

	/**
	 * 获得主键名称
	 * 
	 * @return
	 */
	private String getEntityId() {
		String simpleName = entityClass.getSimpleName();
		return String.valueOf(simpleName.charAt(0)).toLowerCase() + simpleName.substring(1) + "Id";
	}

	@Override
	public Long getEntityIdValue(Object entity) {
		try {
			return (Long) PropertyUtils.getProperty(entity, getEntityId());
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}

		return null;
	}

	@Override
	public void saveOrUpdate(T entity) {
		this.getHibernateTemplate().saveOrUpdate(entity);
	}

	@Override
	public void saveOrUpdateAll(Collection<T> entities) {
		entities.forEach(each -> {
			this.getHibernateTemplate().saveOrUpdate(each);
		});
	}

	// TODO: 这个不知道会不会有问题，先试下
	@Override
	public Session getCurrentSession() {
			return currentSession();
	}

	@Override
	public void delete(T entity) {
		getHibernateTemplate().delete(entity);

		getOperateLoggerService().logDelete(entity);
	}

	@Override
	public void delete(Collection<T> entities) {
		if (entities.size() == 0)
			return;

		for (T entity : entities) {
			this.delete(entity);
		}
	}

	@Override
	public void delete(String entityIds) {
		if (StringUtils.hasText(entityIds)) {
			for (String entityId : entityIds.split(",")) {
				this.delete(this.get(Long.valueOf(entityId)));
			}
		}
	}

	@Override
	public T get(Long id) {
		return getHibernateTemplate().get(entityClass, id);
	}

	@Override
	public List<T> query(Long[] ids) {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.in(getEntityId(), ids));
		return (List<T>) this.intervene(criteria.list());
	}

	@Override
	public List<T> queryAll() {
		return (List<T>) this.intervene(getHibernateTemplate().loadAll(entityClass));
	}

	@Override
	public List<T> query(ConditionModel model) {
		return (List<T>) this.intervene(this.query(model, Criteria.ROOT_ENTITY));
	}

	@Override
	public List<T> query(ConditionModel model, ResultTransformer resultTransformer) {
		Criteria criteria = this.createCriteria(model);
		return (List<T>) this.intervene(this.query(criteria, resultTransformer));
	}

	@Override
	public List<T> query(Criteria criteria) {
		return (List<T>) this.intervene(this.query(criteria, Criteria.ROOT_ENTITY));
	}

	@Override
	public List<T> query(Criteria criteria, ResultTransformer resultTransformer) {
		if (criteria == null)
			return new ArrayList<T>();

		criteria.addOrder(Order.desc(getEntityId()));
		criteria.setResultTransformer(resultTransformer);

		return (List<T>) this.intervene(criteria.list());
	}

	@Override
	public PaginationSupport paging(ConditionModel model) {
		return this.paging(model, Criteria.ROOT_ENTITY);
	}

	@Override
	public PaginationSupport paging(ConditionModel model, ResultTransformer resultTransformer) {
		return this.paging(this.createCriteria(model), model.getStart(), model.getLimit(), resultTransformer);
	}

	@Override
	public PaginationSupport paging(Criteria criteria, int start, int limit) {
		return this.paging(criteria, start, limit, Criteria.ROOT_ENTITY);
	}

	@Override
	public PaginationSupport paging(Criteria criteria, int start, int limit, ResultTransformer resultTransformer) {
		PaginationSupport paging = new PaginationSupport();

		if (criteria != null) {
//
			// 统计 记录总数
			criteria.setProjection(Projections.rowCount());
			paging.setTotal(Integer.valueOf(String.valueOf(criteria.uniqueResult())) );

			criteria.addOrder(Order.desc(getEntityId()));

			// 统计分页结果
			criteria.setProjection(null);
			criteria.setFirstResult(start);
			criteria.setMaxResults(limit);
			criteria.setResultTransformer(resultTransformer);
			paging.setPaginationResults((List<T>) this.intervene(criteria.list()));
		} else {
			paging.setPageNumber(1);
		}

		return paging;
	}

	@Override
	public void save(T entity) {
		getHibernateTemplate().save(entity);

		getOperateLoggerService().logCreate(entity);
	}

	@Override
	public void update(T entity) {
		getHibernateTemplate().update(entity);

		getOperateLoggerService().logUpdate(entity);
	}

	@Override
	public void update(T entity, OperateAction operateAction) {
		if (operateAction == null) {
			this.update(entity);

		} else {
			getHibernateTemplate().update(entity);

			getOperateLoggerService().log(operateAction, entity);
		}
	}


	@Override
	public void merge(T entity) {
		getHibernateTemplate().merge(entity);
	}

	@Deprecated
	@Override
	public Criteria createCriteria() {
		return getCurrentSession().createCriteria(entityClass);
	}
	@Deprecated
	@Override
	public Criteria createCriteria(ConditionModel model) {
		return createCriteria(entityClass, model);
	}



	@Override
	public int count(ConditionModel cm) {
		Criteria criteria = createCriteria(cm);
		return count(criteria);
	}

	@Override
	public int count(Criteria criteria) {
		if (criteria != null) {
			criteria.setProjection(Projections.rowCount());
			Number count = (Number) criteria.uniqueResult();
			return count == null ? 0 : count.intValue();
		}

		return 0;
	}

	@Override
	public void flush() {
		getHibernateTemplate().flush();
	}

	@Override
	public void clear() {
		getHibernateTemplate().clear();
	}

	@Override
	public T execute(HibernateCallback<T> action) {
		return getHibernateTemplate().execute(action);
	}

	@Override
	public List<T> executeFind(HibernateCallback<?> action) {
		return (List<T>) this.intervene((List)getHibernateTemplate().execute(action));
	}

	@Override
	public void evict(T entity) {
		getHibernateTemplate().evict(entity);
	}

	@Override
	public T intervene(T entity) {
		return entity;
	}

	@Override
	public Collection<T> intervene(Collection<T> entities) {
		for (T entity : entities) {
			entity = this.intervene(entity);
		}

		return entities;
	}

	@Override
	public boolean isAuthorizedUserGroup(AuthenticationUserGroup aug, OperateAction operateAction) {
		LoggerEntity loggerEntity = null;
		String dataDesc = "数据";

		loggerEntity = (LoggerEntity) aug;
		dataDesc = " \"" + loggerEntity.getEntityName() + "[" + loggerEntity.getLoggerName() + "]\"";

		User user = null;
		UserGroup userGroup = null;
		AuthenticationUserDetails aud = this.getPrincipal();

		try {
			// 用户未登录则对指定用户组就无任何操作权限
			if (aud == null) {
				throw new Warning("用户未登录不允许<" + operateAction.value() + ">" + dataDesc);
			}

			// 登录用户的用户组为空则无任何操作权限
			if (aud.getUserGroupId() == null) {
				throw new Warning("管理员未给用户(" + aud.getRealname() + ")分配用户组,该用户无权<" + operateAction.value() + ">" + dataDesc);
			}

			// 如果是超级用户组则拥有所有权限
			if (aud.isUserGroupAdministrator()) {
				return true;
			}
			//add by zhoushasha 2015/6/19
			if (aud.isAdministrator()) {
				return true;
			}
			String self = "用户(" + aud.getRealname() + "," + aud.getUserGroupName() + ")";

			// 未提供需要校验的用户ID则无任何操作权限
			if (aug.getUserId() == null) {
				throw new Warning("未能识别到" + dataDesc + " 的创建人," + self + "无权<" + operateAction.value() + ">");
			}

			user = context.getBean(UserService.class).get(aug.getUserId());

			// 获得指定用户组
			userGroup = context.getBean(UserGroupRelationService.class).getUserGroupByUser(aug.getUserId());

			// 记录创建者未分配用户组则无任何操作权限
			if (userGroup == null) {
				throw new Warning("管理员未给用户(" + user.getRealName() + ")分配用户组," + self + "无权<" + operateAction + ">" + dataDesc);
			}

			// 校验当前用户组对指定用户组及子用户组是否有操作权限
			if (!Pattern.matches("^" + aud.getUserGroupId() + ".*", String.valueOf(userGroup.getUserGroupId()))) {
				StringBuilder message = new StringBuilder();
				// message.append("用户(").append(aud.getRealname()).append(",").append(aud.getUserGroupName()).append(")");
				message.append(self);
				message.append("无权对属于");
				message.append("用户(").append(user.getRealName()).append(",").append(userGroup.getName()).append(")");
				message.append("的").append(dataDesc);
				message.append("进行<").append(operateAction.value()).append(">操作");
				throw new Warning(message.toString());
			}

		} catch (Warning e) {
			this.getOperateLoggerService().logUnauthorized(e.getMessage());

			throw e;
		}

		return true;
	}

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

	protected OperateLoggerService getOperateLoggerService() {
		return context.getBean(OperateLoggerService.class);
	}

	/**
	 * 创建查询条件
	 * 
	 * @param clazz
	 * @param model
	 * @return
	 */
	private Criteria createCriteria(Class<?> clazz, ConditionModel model) {
		if (clazz == null || model == null/* || model.getConditionSize() == 0*/)
			return null;

		Criteria criteria = getCurrentSession().createCriteria(clazz);

		// 创建别名
		for (String alias : model.getAliasKey())
			criteria.createAlias(alias, model.getAliasValue(alias), Criteria.LEFT_JOIN);

		// 创建条件
		for (String column : model.getColumns()) {
			Collection<Criterion> criterions = this.createCriterion(clazz, column, model);
			if (criterions != null && criterions.size() > 0) {
				for (Criterion criterion : criterions) {
					if (criterion != null) {
						criteria.add(criterion);
					}
				}
			}
			/*Criterion criterion = this.createCriterion(clazz, column, model);
			if (criterion != null)
				criteria.add(criterion);*/
		}

		criteria.setProjection(null).setResultTransformer(Criteria.ROOT_ENTITY);

		// 创建排序
		for (OrderBy orderBy : model.getOrderBys()) {
			if (orderBy.isAsc()) {
				criteria.addOrder(Order.asc(orderBy.getColumn()));
			} else {
				criteria.addOrder(Order.desc(orderBy.getColumn()));
			}
		}

		return criteria;
	}

	/**
	 * 创建表达式
	 * 
	 * @param clazz
	 * @param column
	 * @param model
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	private Collection<Criterion> createCriterion(Class<?> clazz, String column, ConditionModel model) {
		Collection<ConditionExpression> conditions = model.getConditions(column);
		if (conditions == null || conditions.size() == 0)
			return null;

		Collection<Criterion> criterions = new ArrayList<Criterion>(conditions.size());
		for (ConditionExpression condition : conditions) {
			String alias = model.getAliasValue(column);
			String op = condition.getOp();
			Object value = condition.getValue();

			if (value == null || (value instanceof String && !StringUtils.hasText((String) value))) {
				continue;
			}

			Class<?> type = BeanUtil.findFieldType(clazz, column);
			if (type == null) {
				continue;
			}

			Criterion criterion = null;
			Object v = null;
			v = ConvertUtils.convert(value, type);

			if (ConditionExpression.LK.equals(op)) {
				if (type.isAssignableFrom(String.class)) {
					MatchMode mode = MatchMode.ANYWHERE;
					if (ConditionExpression.START_MATCH.equals(condition.getMatchMode()))
						mode = MatchMode.START;
					else if (ConditionExpression.END_MATCH.equals(condition.getMatchMode()))
						mode = MatchMode.END;

					criterion = Restrictions.like(alias, (String) v, mode);

				} else {
					// 对非字符串字段进行Like查询
					String val = String.valueOf(v);
					if (ConditionExpression.START_MATCH.equals(condition.getMatchMode())) {
						val += "%";
					} else if (ConditionExpression.END_MATCH.equals(condition.getMatchMode())) {
						val = "%" + val;
					} else {
						val = "%" + val + "%";
					}

					criterion = Restrictions.sqlRestriction("{alias}." + alias.replaceAll("\\.", "_") + " like ?", val,
							StandardBasicTypes.STRING);
				}

			} else if (ConditionExpression.EQ.equals(op)) {
				criterion = Restrictions.eq(alias, v);

			} else if (ConditionExpression.NEQ.equals(op)) {
				criterion = Restrictions.not(Restrictions.eq(alias, v));

			} else if (ConditionExpression.GE.equals(op)) {
				criterion = Restrictions.ge(alias, v);

			} else if (ConditionExpression.GT.equals(op)) {
				criterion = Restrictions.gt(alias, v);

			} else if (ConditionExpression.LE.equals(op)) {
				criterion = Restrictions.le(alias, v);

			} else if (ConditionExpression.LT.equals(op)) {
				criterion = Restrictions.lt(alias, v);

			} else if (ConditionExpression.NULL.equals(op)) {
				criterion = Restrictions.isNull(alias);

			} else if (ConditionExpression.NOT_NULL.equals(op)) {
				criterion = Restrictions.and(Restrictions.isNotNull(alias), Restrictions.not(Restrictions.eq(alias, "")));

			} else if (ConditionExpression.IN.equals(op)) {
				if (value instanceof String) {
					String[] splits = ((String) value).split(",");
					Object[] values = new Object[splits.length];
					for (int i = 0; i < splits.length; i++) {
						String val = splits[i];
						if (StringUtils.hasText(val)) {
							values[i] = ConvertUtils.convert(splits[i], type);
						}
					}
					criterion = Restrictions.in(alias, values);
				} else if (value instanceof Collection) {
					criterion = Restrictions.in(alias, (Collection) value);
				} else if (value instanceof Object[]) {
					criterion = Restrictions.in(alias, (Object[]) value);
				}

			} else if (ConditionExpression.NIN.equals(op)) {
				if (value instanceof String) {
					String[] splits = ((String) value).split(",");
					Object[] values = new Object[splits.length];
					for (int i = 0; i < splits.length; i++) {
						String val = splits[i];
						if (StringUtils.hasText(val)) {
							values[i] = ConvertUtils.convert(splits[i], type);
						}
					}
					criterion = Restrictions.not(Restrictions.in(alias, values));
				} else if (value instanceof Collection) {
					criterion = Restrictions.not(Restrictions.in(alias, (Collection) value));
				} else if (value instanceof Object[]) {
					criterion = Restrictions.not(Restrictions.in(alias, (Object[]) value));
				}
			}

			criterions.add(criterion);
		}

		return criterions;
	}

	@Override
	public void setApplicationContext(ApplicationContext context) throws BeansException {
		this.context = context;
	}
}
