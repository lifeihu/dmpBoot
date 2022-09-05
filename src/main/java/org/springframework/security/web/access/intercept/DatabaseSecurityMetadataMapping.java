package org.springframework.security.web.access.intercept;

import java.util.Map;

public interface DatabaseSecurityMetadataMapping {
	/**
	 * 从数据库中获取访问权限
	 * 
	 * @return
	 */
	public Map<String, String> loadDatabaseSecurityMetadataMapping();
}
