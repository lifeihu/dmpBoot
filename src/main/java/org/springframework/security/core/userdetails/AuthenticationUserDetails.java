package org.springframework.security.core.userdetails;

import java.util.Collection;
import java.util.Date;

import org.springframework.security.core.GrantedAuthority;

public class AuthenticationUserDetails implements UserDetails {

	private static final long serialVersionUID = -2283536499770106L;

	private Long id;
	private String realname;
	private String username;
	private String password;
	private Date lastLoginDate;
	private String ip;
	private boolean administrator;
	private Long userGroupId;
	private String userGroupName;
	private boolean userGroupAdministrator;
	private boolean etl;
	private String vertifyCode;
	private Collection<GrantedAuthority> authorities;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getRealname() {
		return realname;
	}

	public void setRealname(String realname) {
		this.realname = realname;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public Date getLastLoginDate() {
		return lastLoginDate;
	}

	public void setLastLoginDate(Date lastLoginDate) {
		this.lastLoginDate = lastLoginDate;
	}

	public boolean isAdministrator() {
		return administrator;
	}

	public void setAdministrator(boolean administrator) {
		this.administrator = administrator;
	}

	public boolean isEtl() {
		return etl;
	}

	public void setEtl(boolean etl) {
		this.etl = etl;
	}

	public String getVertifyCode() {
		return vertifyCode;
	}

	public void setVertifyCode(String vertifyCode) {
		this.vertifyCode = vertifyCode;
	}

	public void setAuthorities(Collection<GrantedAuthority> authorities) {
		this.authorities = authorities;
	}

	public Collection<GrantedAuthority> getAuthorities() {
		return this.authorities;
	}

	public String getPassword() {
		return this.password;
	}

	public String getUsername() {
		return this.username;
	}

	public boolean isAccountNonExpired() {
		return true;
	}

	public boolean isAccountNonLocked() {
		return true;
	}

	public boolean isCredentialsNonExpired() {
		return true;
	}

	public boolean isEnabled() {
		return true;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public Long getUserGroupId() {
		return userGroupId;
	}

	public void setUserGroupId(Long userGroupId) {
		this.userGroupId = userGroupId;
	}

	public String getUserGroupName() {
		return userGroupName;
	}

	public void setUserGroupName(String userGroupName) {
		this.userGroupName = userGroupName;
	}

	public boolean isUserGroupAdministrator() {
		return userGroupAdministrator;
	}

	public void setUserGroupAdministrator(boolean userGroupAdministrator) {
		this.userGroupAdministrator = userGroupAdministrator;
	}

}
