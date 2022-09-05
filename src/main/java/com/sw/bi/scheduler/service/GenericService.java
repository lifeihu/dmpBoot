package com.sw.bi.scheduler.service;

import com.sw.bi.scheduler.model.AuthenticationUserGroup;
import com.sw.bi.scheduler.util.OperateAction;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.transform.ResultTransformer;
import org.springframework.orm.hibernate5.HibernateCallback;
import org.springframework.ui.ConditionModel;
import org.springframework.ui.PaginationSupport;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public interface GenericService<T> extends Serializable {
	public Session getCurrentSession();

	public void delete(T entity);

	public void delete(String entityIds);

	public void delete(Collection<T> entities);

	public T get(Long id);

	public List<T> queryAll();

	public List<T> query(Long[] ids);

	public List<T> query(ConditionModel model);

	public List<T> query(ConditionModel model, ResultTransformer resultTransformer);

	public List<T> query(Criteria criteria);

	public List<T> query(Criteria criteria, ResultTransformer resultTransformer);

	public PaginationSupport paging(ConditionModel model);

	public PaginationSupport paging(ConditionModel model, ResultTransformer resultTransformer);

	public PaginationSupport paging(Criteria criteria, int start, int limit);

	public PaginationSupport paging(Criteria criteria, int start, int limit, ResultTransformer resultTransformer);

	public void save(T entity);

	public void update(T entity);

	public void update(T entity, OperateAction operateAction);



	public void merge(T entity);

	public Criteria createCriteria();

	public Criteria createCriteria(ConditionModel model);

	public int count(ConditionModel cm);

	public int count(Criteria criteria);

	public void flush();

	public T execute(HibernateCallback<T> action);

//	public List executeFind(HibernateCallback<?> action);

	public void evict(T entity);

	public void clear();

	/**
	 * 数据干预(因为项目中没有创建外键,所以在传往前台时需要将一些只有ID的外键值从数据库中查询相应的对象)
	 * 
	 * @param entity
	 * @return
	 */
	public T intervene(T entity);

	/**
	 * 数据干预(因为项目中没有创建外键,所以在传往前台时需要将一些只有ID的外键值从数据库中查询相应的对象)
	 * 
	 * @param entities
	 * @return
	 */
	public Collection<T> intervene(Collection<T> entities);

	/**
	 * 校验当前登录用户是否有指定用户组的操作权限
	 * 
	 * @param aug
	 * @param operateAction
	 * @return
	 */
	public boolean isAuthorizedUserGroup(AuthenticationUserGroup aug, OperateAction operateAction);

	/**
	 * 获得主键值
	 * 
	 * @param entity
	 * @return
	 */
	public Long getEntityIdValue(Object entity);


	@Deprecated
	public void saveOrUpdate(T entity);

	@Deprecated
	public void saveOrUpdateAll(Collection<T> entities);

	public List executeFind(HibernateCallback<?> action);

}
