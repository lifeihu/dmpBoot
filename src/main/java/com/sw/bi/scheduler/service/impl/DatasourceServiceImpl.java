package com.sw.bi.scheduler.service.impl;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mysql.cj.util.StringUtils;
import com.sw.bi.scheduler.background.util.DxDESCipher;
import com.sw.bi.scheduler.model.Datasource;
import com.sw.bi.scheduler.model.User;
import com.sw.bi.scheduler.model.UserGroup;
import com.sw.bi.scheduler.service.DatasourceService;
import com.sw.bi.scheduler.service.UserGroupRelationService;
import com.sw.bi.scheduler.service.UserGroupService;
import com.sw.bi.scheduler.service.UserService;
import com.sw.bi.scheduler.util.EnumUtil.DatasourceViewType;
import com.sw.bi.scheduler.util.OperateAction;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.transform.ResultTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.resolver.Warning;
import org.springframework.stereotype.Service;
import org.springframework.ui.ConditionModel;
import org.springframework.ui.PaginationSupport;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Service("datasourceService")
public class DatasourceServiceImpl extends GenericServiceHibernateSupport<Datasource> implements DatasourceService {

	@Autowired
	private UserService userService;

	@Autowired
	private UserGroupService userGroupService;

	@Autowired
	private UserGroupRelationService userGroupRelationService;

	@Override
	public String changePassword(long datasourceId, String password) {
		Datasource datasource = get(datasourceId);

		try {
			datasource.setPassword(DxDESCipher.EncryptDES(password, datasource.getUsername()));
			update(datasource, OperateAction.PASSWORD);

		} catch (Exception e) {
			throw new Warning("?????????(" + datasource.getName() + "(" + datasource.getUsername() + "))??????????????????");
		}

		return password;
	}

	@Override
	public void batchChangePassword() {
		Collection<Datasource> datasources = this.queryAll();
		try {
			for (Datasource datasource : datasources) {
				datasource.setPassword(DxDESCipher.EncryptDES(datasource.getPassword(), datasource.getUsername()));
				getHibernateTemplate().update(datasource);
			}
		} catch (Exception e) {
			throw new Warning("???????????????????????????");
		}

	}
	
	@SuppressWarnings("deprecation")
	@Override
	public String testDatasource(Datasource datasource) {
		String driverClass = null;
		int type = datasource.getType().intValue();

		FTPClient client = null;
		Connection connection = null;
		Statement stmt = null;
		ResultSet rs = null;
		String result = "";
		MongoClient mongoClient=null;
		try {
			
			String password = datasource.getPassword();
			Criteria criteria = createCriteria().add(Restrictions.eq("ip", datasource.getIp())).add(Restrictions.eq("databaseName", datasource.getDatabaseName())).add(Restrictions.eq("username", datasource.getUsername()));
			if (criteria.list().size() > 0){
				password = DxDESCipher.DecryptDES(((Datasource)criteria.list().iterator().next()).getPassword(), datasource.getUsername());
			}
			
			if (type == 3) {
				// FTP?????????
				client = new FTPClient();
				client.connect(datasource.getIp(), Integer.parseInt(datasource.getPort()));
				client.login(datasource.getUsername(), password/*DxDESCipher.DecryptDES(datasource.getPassword(), datasource.getUsername())*/);

				int reply = client.getReplyCode();
				if (!FTPReply.isPositiveCompletion(reply)) {
					client.disconnect();
					return "???????????????????????? - FTP?????????????????????.";
				}

				client.logout();

				result = "FTP?????????(" + datasource.getIp() + ").";

			}else if(type==9){ 
				//mongodb?????????
			
				ServerAddress serverAddress=new ServerAddress(datasource.getIp(),Integer.parseInt(datasource.getPort()));
				 if(!StringUtils.isNullOrEmpty(datasource.getUsername()) && !StringUtils.isNullOrEmpty(password)) {
					   mongoClient = new MongoClient(serverAddress, Arrays.asList(MongoCredential.createCredential(datasource.getUsername(), datasource.getDatabaseName(), password.toCharArray())));
			            
//						    String sURI = String.format("mongodb://%s:%s@%s:%d/%s", username, password, ip, 27017, database); 
//							MongoClientURI uri = new MongoClientURI(sURI); 
//							this.mongoClient = new MongoClient(uri);
			         } else {
			             mongoClient = new MongoClient(serverAddress);
			           
			         }
				DB db1=mongoClient.getDB(datasource.getDatabaseName());
				if(db1.getStats() != null){
					result=mongoClient.getConnectPoint();
				}
			}else {
				// DB?????????

				String testSql = null;
				if (type == 0) {
					testSql = "select concat('MySQL ', version())";
					driverClass = "com.mysql.jdbc.Driver";
				}else if (type == 1) {
					testSql = "select @@version";
					driverClass = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
				}else if (type == 2) {
                    testSql = "Select * From v$version";
                    driverClass = "oracle.jdbc.driver.OracleDriver";
                }else if (type == 7) {
					testSql = "select version()";
					driverClass = "org.postgresql.Driver";
				} else if (type == 10) {
					testSql = "SELECT  1 FROM DUAL";
					driverClass = "sunje.goldilocks.jdbc.GoldilocksDriver";
				}
				
				/*String password = datasource.getPassword();
				Criteria criteria = createCriteria().add(Restrictions.eq("ip", datasource.getIp())).add(Restrictions.eq("databaseName", datasource.getDatabaseName())).add(Restrictions.eq("username", datasource.getUsername()));
				if (criteria.list().size() > 0){
					password = DxDESCipher.DecryptDES(((Datasource)criteria.list().iterator().next()).getPassword(), datasource.getUsername());
				}*/

				Class.forName(driverClass);
				connection = DriverManager.getConnection(datasource.getConnectionString(), datasource.getUsername(), password/*DxDESCipher.DecryptDES(datasource.getPassword(), datasource.getUsername())*/);
				stmt = connection.createStatement();
				rs = stmt.executeQuery(testSql);

				if (rs.next()) {
					result = rs.getString(1);
				}

			}

			result = "????????????????????? - " + result;

		} catch (Exception e) {
			result = "????????????????????? - " + e.getMessage();
			e.printStackTrace();

		} finally {
			try {
				if (client != null) {
					client.disconnect();
				}
				if(mongoClient!=null){
					mongoClient.close();
				}
				if (rs != null) {
					rs.close();
				}

				if (stmt != null) {
					stmt.close();
				}

				if (connection != null) {
					connection.close();
				}
			} catch (Exception e) {}
		}

		return result;
	}

	@Override
	public void logicDelete(Datasource datasource) {
		if (datasource == null) {
			return;
		}

		datasource.setActive(false);
		super.update(datasource, OperateAction.LOGIC_DELETE);
	}

	@Override
	public void recovery(Datasource datasource) {
		if (datasource == null) {
			return;
		}

		datasource.setActive(true);
		super.update(datasource, OperateAction.RECOVERY);
	}

	@Override
	public List<Datasource> query(ConditionModel model, ResultTransformer resultTransformer) {
		/*Long userId = model.getValue("userId", Long.class);
		model.removeCondition("userId");*/

		Long userGroupId = model.getValue("userGroupId", Long.class);
		model.removeCondition("userGroupId");

		if (userGroupId == null) {
			return new ArrayList<Datasource>();
		}

		// ???????????????????????????????????????????????????????????????????????????
		// ?????????????????????????????????????????????????????????????????????????????????(?????????????????????????????????????????????)
		Collection<Long> userIds = new ArrayList<Long>();

		// ???????????????????????????????????????????????????
		UserGroup userGroup = userGroupService.get(userGroupId);

		// ??????????????????????????????????????????
		if (!userGroup.isAdministrator()) {
			Collection<User> users = userGroupRelationService.getUsersByUserGroup(userGroupId, true);
			for (User user : users) {
				userIds.add(user.getUserId());
			}
		}

		Criteria criteria = this.createCriteria(model);
		criteria.addOrder(Order.desc("active"));

		if (/*!userService.isAdministrator(userId) || */userGroup.isAdministrator()) {
			// ???????????????????????????????????????????????????????????????????????????????????????????????????
			// criteria.add(Restrictions.or(Restrictions.eq("viewType", DatasourceViewType.ALL_USER.ordinal()), Restrictions.eq("createBy", userId)));
			criteria.add(Restrictions.eq("viewType", DatasourceViewType.ALL_USER.ordinal()));
		}

		if (userIds.size() > 0) {
			criteria.add(Restrictions.in("createBy", userIds));
		}

		criteria.addOrder(Order.asc("type"));

		return super.query(criteria, resultTransformer);
	}

	@Override
	public PaginationSupport paging(ConditionModel model, ResultTransformer resultTransformer) {
		/*Long userId = model.getValue("userId", Long.class);
		model.removeCondition("userId");*/

		Long userGroupId = model.getValue("userGroupId", Long.class);
		model.removeCondition("userGroupId");

		if (userGroupId == null) {
			return new PaginationSupport(model.getStart(), model.getLimit());
		}

		// ???????????????????????????????????????????????????????????????????????????
		// ?????????????????????????????????????????????????????????????????????????????????(?????????????????????????????????????????????)
		Collection<Long> userIds = new ArrayList<Long>();

		UserGroup userGroup = userGroupService.get(userGroupId);

		// ??????????????????????????????????????????
		if (!userGroup.isAdministrator()) {
			// ???????????????????????????????????????????????????
			Collection<User> users = userGroupRelationService.getUsersByUserGroup(userGroupId, true);
			for (User user : users) {
				userIds.add(user.getUserId());
			}
		}

		Criteria criteria = this.createCriteria(model);
//		criteria.addOrder(Order.desc("active"));

		if (/*!userService.isAdministrator(userId)*/userGroup.isAdministrator()) {
			// ???????????????????????????????????????????????????????????????????????????????????????????????????
			// criteria.add(Restrictions.or(Restrictions.eq("viewType", DatasourceViewType.ALL_USER.ordinal()), Restrictions.eq("createBy", userId)));
			criteria.add(Restrictions.eq("viewType", DatasourceViewType.ALL_USER.ordinal()));
		}

		if (userIds.size() > 0) {
			criteria.add(Restrictions.in("createBy", userIds));
		}

		return super.paging(criteria, model.getStart(), model.getLimit(), resultTransformer);
	}

	@Override
	public void saveOrUpdate(Datasource datasource) {
		Criteria criteria = createCriteria();
		criteria.add(Restrictions.eq("name", datasource.getName()));
		if (datasource.getDatasourceId() != null) {
			criteria.add(Restrictions.not(Restrictions.eq("datasourceId", datasource.getDatasourceId())));
		}
		Datasource d = (Datasource) criteria.uniqueResult();

		if (d != null) {
			throw new Warning("?????????(" + datasource.getName() + ")????????????!");
		}

		if (datasource.getDatasourceId() == null) {
			try {
				datasource.setPassword(DxDESCipher.EncryptDES(datasource.getPassword(), datasource.getUsername()));
			} catch (Exception e) {
				throw new Warning("?????????(" + datasource.getName() + ")??????????????????");
			}

		} else {
			Datasource oldDatasource = this.get(datasource.getDatasourceId());
			getHibernateTemplate().evict(oldDatasource);

			datasource.setPassword(oldDatasource.getPassword());
		}

		super.saveOrUpdate(datasource);
	}

	@Override
	public Datasource intervene(Datasource datasource) {
		datasource.setUserGroup(userGroupRelationService.getUserGroupByUser(datasource.getCreateBy()));

		return datasource;
	}

	@Override
	public Datasource queryById(Long id) {
		return this.get(id);
	}
}
