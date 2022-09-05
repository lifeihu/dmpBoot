package com.sw.bi.scheduler.service.impl;

import com.sw.bi.scheduler.background.taskexcuter.Parameters.BooleanResult;
import com.sw.bi.scheduler.model.ActionSql;
import com.sw.bi.scheduler.service.ActionSqlService;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.Session;
import org.hibernate.type.StandardBasicTypes;
import org.springframework.orm.hibernate5.HibernateCallback;
import org.springframework.stereotype.Service;
import org.springframework.ui.ConditionModel;
import org.springframework.ui.PaginationSupport;

import java.util.*;
import java.util.Map.Entry;

@Service("actionSqlService")
public class ActionSqlServiceImpl extends GenericServiceHibernateSupport<ActionSql> implements ActionSqlService {

	public long getLastSqlIndex(long actionId) {
		StringBuffer hql = new StringBuffer("from ActionSql a where a.action.actionId =" + actionId + " and a.runResult = " + BooleanResult.FAILED.indexOf());
		System.out.println("getLastSqlIndex: " + hql);
		Query query = getCurrentSession().createQuery(hql.toString());

		List list = query.list();
		if (list.size() == 0) {
			System.out.println("return 1");
			return 1;
		}
		return ((ActionSql) list.get(0)).getSqlIndex();
	}

	@Override
	public PaginationSupport pagingBySql(final ConditionModel cm) {

		return getHibernateTemplate().execute(new HibernateCallback<PaginationSupport>() {

			@Override
			public PaginationSupport doInHibernate(Session session) throws HibernateException {

				Date taskDate = cm.getValue("taskDate", Date.class);
				String dutyMan = cm.getValue("dutyMan", String.class);

				List<String> clauses = new ArrayList<String>();
				Map<String, Object> params = new HashMap<String, Object>();

				params.put("taskDate", taskDate);

				if (dutyMan != null) {
					clauses.add("duty_man = :dutyMan");
					params.put("dutyMan", dutyMan);
				}

				String whereClause = "";
				for (int i = 0; i < clauses.size(); i++) {
					whereClause += " and " + clauses.get(i);
				}

				StringBuffer resultSql = new StringBuffer("select * from action_sql where task_date = :taskDate");
				resultSql.append(whereClause);
				resultSql.append(" and run_time in (");
				resultSql.append("select max_run_time from (select max(run_time) max_run_time from action_sql where task_date = :taskDate");
				resultSql.append(whereClause);
				resultSql.append(" group by task_date, hive_sql_path, sql_string order by max_run_time desc limit 30) as tmp)");
				resultSql.append(" group by task_date, hive_sql_path, sql_string");
				resultSql.append(" order by run_time desc, hive_sql_path");

				SQLQuery query = session.createSQLQuery(resultSql.toString());
				query.setFirstResult(cm.getStart());
				query.setMaxResults(cm.getLimit());
				query.addEntity(ActionSql.class);

				for (Entry<String, Object> entry : params.entrySet()) {
					String key = entry.getKey();
					Object value = entry.getValue();

					query.setParameter(key, value);
				}

				PaginationSupport ps = new PaginationSupport(cm.getStart(), cm.getLimit());

				ps.setPaginationResults(query.list());

				query = session.createSQLQuery("select count(c.action_sql_id) count from (" + resultSql + ") c");
				query.addScalar("count", StandardBasicTypes.INTEGER);

				for (Entry<String, Object> entry : params.entrySet()) {
					String key = entry.getKey();
					Object value = entry.getValue();

					query.setParameter(key, value);
				}

				ps.setTotal((Integer) query.uniqueResult());

				return ps;
			}

		});
	}
}
