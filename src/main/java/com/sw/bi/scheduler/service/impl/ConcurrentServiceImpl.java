package com.sw.bi.scheduler.service.impl;

import com.sw.bi.scheduler.model.Concurrent;
import com.sw.bi.scheduler.service.ConcurrentService;
import org.hibernate.Criteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

@Service
@SuppressWarnings("unchecked")
public class ConcurrentServiceImpl extends GenericServiceHibernateSupport<Concurrent> implements ConcurrentService {

	private static Map<Integer, Concurrent> categoryConcurrentMapping = new HashMap<Integer, Concurrent>();

	@Override
	public int getMaxConcurrentNumberByCategory(int category) {
		Concurrent concurrent = this.getConcurrentByCategory(category);

		return concurrent == null ? 0 : concurrent.getRunningMaxConcurrentNumber();
	}

	@Override
	public int getBigDataMaxConcurrentNumberByCategory(int category) {
		Concurrent concurrent = this.getConcurrentByCategory(category);

		return concurrent == null ? 0 : concurrent.getRunningBigDataMaxConcurrentNumber();
	}

	@Override
	public String getCategoryName(int category) {
		Concurrent concurrent = this.getConcurrentByCategory(category);

		return concurrent == null ? "" : concurrent.getName();
	}

	@Override
	public Collection<Integer> getConcurrentCategories() {
		Criteria criteria = this.createCriteria();
		criteria.setProjection(Projections.distinct(Projections.property("category")));

		return criteria.list();
	}

	@Override
	public Integer getConcurrentCategory(long jobType) {
		Criteria criteria = this.createCriteria();
		criteria.add(Restrictions.eq("jobType", jobType));
		criteria.setMaxResults(1); // TODO 这里事先必须保证一个作业类型只能属于一个分类

		Concurrent concurrent = (Concurrent) criteria.uniqueResult();
		return concurrent == null ? null : concurrent.getCategory();
	}

	@Override
	public Collection<Long> getConcurrentJobTypes() {
		Criteria criteria = this.createCriteria();
		criteria.setProjection(Projections.distinct(Projections.property("jobType")));

		return criteria.list();
	}

	@Override
	public Collection<Long> getConcurrentJobTypes(int category) {
		Criteria criteria = this.createCriteria();
		criteria.add(Restrictions.eq("category", category));
		criteria.setProjection(Projections.property("jobType"));

		return criteria.list();
	}

	private Concurrent getConcurrentByCategory(int category) {
		if (categoryConcurrentMapping.containsKey(category)) {
			return categoryConcurrentMapping.get(category);
		}

		Criteria criteria = this.createCriteria();
		criteria.add(Restrictions.eq("category", category));
		criteria.setMaxResults(1);
		Concurrent concurrent = (Concurrent) criteria.uniqueResult();

		if (concurrent != null) {
			categoryConcurrentMapping.put(category, concurrent);
		}

		return concurrent;
	}

}
