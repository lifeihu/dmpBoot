package com.sw.bi.scheduler.service.impl;

import com.sw.bi.scheduler.model.Job;
import com.sw.bi.scheduler.model.JobRelation;
import com.sw.bi.scheduler.service.JobRelationService;
import com.sw.bi.scheduler.service.UserGroupRelationService;
import com.sw.bi.scheduler.util.Configure.JobStatus;
import com.sw.bi.scheduler.util.Configure.TaskStatus;
import com.sw.bi.scheduler.util.DateUtil;
import org.hibernate.*;
import org.hibernate.criterion.Restrictions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate5.HibernateCallback;
import org.springframework.stereotype.Service;

import java.util.*;

@Service("jobRelationService")
@SuppressWarnings("unchecked")
public class JobRelationServiceImpl extends GenericServiceHibernateSupport<JobRelation> implements JobRelationService {

	@Autowired
	private UserGroupRelationService userGroupRelationService;

	@Override
	public void createRelation(long jobId, Long[] relationJobIds) {
		Date today = DateUtil.getToday();
		for (long relationJobId : relationJobIds) {
			JobRelation jr = new JobRelation(jobId, relationJobId, today, today);
			save(jr);
		}
	}

	@Override
	public Job getRootJob() {

		return getHibernateTemplate().execute(new HibernateCallback<Job>() {

			@Override
			public Job doInHibernate(Session session) throws HibernateException {
				String sql = "select {j.*} from job_relation jr left join job j on jr.job_id = j.job_id where jr.parent_id is null";

				SQLQuery query = session.createSQLQuery(sql);
				query.addEntity("j", Job.class);

				return (Job) query.uniqueResult();
			}

		});
	}

	/**
	 * 获得指定作业的在线父作业的集合
	 */
	@Override
	public List<Job> getOnlineParentJobs(final long jobId) {
		if (jobId == 1l) {
			return new ArrayList<Job>();
		}

		return getHibernateTemplate().execute(new HibernateCallback<List<Job>>() {

			@Override
			public List<Job> doInHibernate(Session session) throws HibernateException {
				StringBuffer sql = new StringBuffer("select {j.*} from job_relation jr left join job j on jr.parent_id = j.job_id ");
				sql.append("where jr.job_id = :jobId ");
				sql.append("and j.job_status = :jobStatus ");

				SQLQuery query = session.createSQLQuery(sql.toString());
				query.addEntity("j", Job.class);
				query.setLong("jobId", jobId);
				query.setLong("jobStatus", (long) JobStatus.ON_LINE.indexOf());

				return query.list();
			}

		});
	}

	@Override
	public Collection<Job> getOnlineDepthParentJobsToCollection(long jobId, Integer depth) {
		Collection<Job> parents = new HashSet<Job>();

		if (depth == null) {
			depth = Integer.MAX_VALUE;
		}

		if (depth == 0) {
			return parents;
		}

		List<Job> parentJobs = this.getOnlineParentJobs(jobId);
		if (parentJobs.size() > 0) {
			parents.addAll(parentJobs);
			depth -= 1;

			for (Job childJob : parentJobs) {
				parents.addAll(this.getOnlineDepthParentJobsToCollection(childJob.getJobId(), depth));
			}
		}

		return parents;
	}

	@Override
	public List<Job> getAllParentJobs(final long jobId) {
		if (jobId == 1l) {
			return new ArrayList<Job>();
		}

		return getHibernateTemplate().execute(new HibernateCallback<List<Job>>() {

			@Override
			public List<Job> doInHibernate(Session session) throws HibernateException {
				StringBuffer sql = new StringBuffer("select {j.*} from job_relation jr left join job j on jr.parent_id = j.job_id ");
				sql.append("where jr.job_id = :jobId ");

				SQLQuery query = session.createSQLQuery(sql.toString());
				query.addEntity("j", Job.class);
				query.setLong("jobId", jobId);

				return query.list();
			}

		});
	}

	@Override
	public List<Job> getOnlineChildrenJobs(final long parentJobId) {
		return getHibernateTemplate().execute(new HibernateCallback<List<Job>>() {

			@Override
			public List<Job> doInHibernate(Session session) throws HibernateException {
				StringBuffer sql = new StringBuffer("select {j.*} from job_relation jr left join job j on jr.job_id = j.job_id ");
				sql.append("where jr.parent_id = :parentJobId ");
				sql.append("and j.job_status = :jobStatus ");

				SQLQuery query = session.createSQLQuery(sql.toString());
				query.addEntity("j", Job.class);
				query.setLong("parentJobId", parentJobId);
				query.setLong("jobStatus", (long) JobStatus.ON_LINE.indexOf());

				return query.list();
			}

		});
	}

	@Override
	public Collection<Job> getOnlineDepthChildrenJobsToCollection(long parentJobId, Integer depth) {
		Collection<Job> children = new HashSet<Job>();

		if (depth == null) {
			depth = Integer.MAX_VALUE;
		}

		if (depth == 0) {
			return children;
		}

		List<Job> childrenJobs = this.getOnlineChildrenJobs(parentJobId);
		if (childrenJobs.size() > 0) {
			children.addAll(childrenJobs);
			depth -= 1;

			for (Job childJob : childrenJobs) {
				childJob.setUserGroup(userGroupRelationService.getUserGroupByUser(childJob.getDutyOfficer()));
				children.addAll(this.getOnlineDepthChildrenJobsToCollection(childJob.getJobId(), depth));
			}
		}

		return children;
	}

	@Override
	public int getUnsuceesParentSize(final long jobId) {

		return getHibernateTemplate().execute(new HibernateCallback<Integer>() {

			@Override
			public Integer doInHibernate(Session session) throws HibernateException {
				StringBuffer hql = new StringBuffer("select count(*) from Task t where t.taskDate = :taskDate");
				hql.append(" and t.taskStatus not in(");
				hql.append(TaskStatus.RE_RUN_SUCCESS.indexOf() + "," + TaskStatus.RUN_SUCCESS.indexOf());
				hql.append(") and t.jobId in ");
				hql.append("(select jr.parentId from JobRelation jr where jr.jobId = :jobId)");

				Query query = session.createQuery(hql.toString());
				query.setLong("jobId", jobId);
				query.setDate("taskDate", new Date());

				return (Integer) query.uniqueResult();
			}

		});
	}

	@Override
	public void removeParentJobsFromJobRelations(long jobId, Collection<Long> parentIds) {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("jobId", jobId));
		if (parentIds != null && parentIds.size() > 0) {
			criteria.add(Restrictions.in("parentId", parentIds));
		}

		List<JobRelation> jobRelations = criteria.list();

		this.delete(jobRelations);
	}

	@Override
	public JobRelation getJobRelationByJob(long jobId) {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("jobId", jobId));

		JobRelation jobRelation = (JobRelation) criteria.uniqueResult();
		return jobRelation;
	}
}
