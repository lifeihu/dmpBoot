package com.sw.bi.scheduler.controller;

import com.sw.bi.scheduler.model.Datasource;
import com.sw.bi.scheduler.service.DatasourceService;
import com.sw.bi.scheduler.service.GenericService;
import com.sw.bi.scheduler.util.OperateAction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Collection;
import java.util.HashSet;

@Controller
@RequestMapping("/manage/datasource")
public class DatasourceController extends BaseActionController<Datasource> {

	@Autowired
	private DatasourceService datasourceService;

	@RequestMapping("/password")
	@ResponseBody
	public String changePassword(long datasourceId, String password) {
		return datasourceService.changePassword(datasourceId, password);
	}

	@RequestMapping("/batchChangePassword")
	public void batchChangePassword() {
		datasourceService.batchChangePassword();
	}

	@RequestMapping("/test")
	@ResponseBody
	public String testDatasource() {
		Datasource datasource = super.decode("datasource", Datasource.class);
		return datasourceService.testDatasource(datasource);
	}

	@RequestMapping("/logicRemove")
	@ResponseBody
	public void logicRemove(String id) {
		if (!StringUtils.hasText(id)) {
			return;
		}

		Collection<Datasource> datasources = new HashSet<Datasource>();
		for (String datasourceId : id.split(",")) {
			Datasource datasource = datasourceService.get(Long.valueOf(datasourceId));
			datasourceService.isAuthorizedUserGroup(datasource, OperateAction.LOGIC_DELETE);

			datasources.add(datasource);
		}

		for (Datasource datasource : datasources) {
			datasourceService.logicDelete(datasource);
		}

	}

	@RequestMapping("/recovery")
	@ResponseBody
	public void recovery(String id) {
		if (!StringUtils.hasText(id)) {
			return;
		}

		Collection<Datasource> datasources = new HashSet<Datasource>();
		for (String datasourceId : id.split(",")) {
			Datasource datasource = datasourceService.get(Long.valueOf(datasourceId));
			datasourceService.isAuthorizedUserGroup(datasource, OperateAction.RECOVERY);

			datasources.add(datasource);
		}

		for (Datasource datasource : datasources) {
			datasourceService.recovery(datasource);
		}
	}

	@Override
	public GenericService<Datasource> getDefaultService() {
		return datasourceService;
	}
}
