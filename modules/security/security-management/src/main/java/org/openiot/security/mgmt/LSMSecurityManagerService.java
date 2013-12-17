/**
 * Copyright (c) 2011-2014, OpenIoT
 *
 * This library is free software; you can redistribute it and/or
 * modify it either under the terms of the GNU Lesser General Public
 * License version 2.1 as published by the Free Software Foundation
 * (the "LGPL"). If you do not alter this
 * notice, a recipient may use your version of this file under the LGPL.
 *
 * You should have received a copy of the LGPL along with this library
 * in the file COPYING-LGPL-2.1; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * This software is distributed on an "AS IS" basis, WITHOUT WARRANTY
 * OF ANY KIND, either express or implied. See the LGPL  for
 * the specific language governing rights and limitations.
 *
 * Contact: OpenIoT mailto: info@openiot.eu
 */

package org.openiot.security.mgmt;

import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Named;

import org.openiot.lsm.security.oauth.LSMOAuthHttpManager;
import org.openiot.lsm.security.oauth.mgmt.Permission;
import org.openiot.lsm.security.oauth.mgmt.Role;
import org.openiot.lsm.security.oauth.mgmt.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.sparql.engine.http.QueryEngineHTTP;

/**
 * This class is responsible for retrieving and persisting OpenIoT authentication and access control
 * management objects.
 * 
 * @author Mehdi Riahi
 * 
 */
@Named("securityManagerService")
@Stateless
public class LSMSecurityManagerService {

	private static Logger logger = LoggerFactory.getLogger(LSMSecurityManagerService.class);

	static String OAuthGraphURL = "http://lsm.deri.ie/OpenIoT/OAuth#";
	private static LSMSecurityManagerService instance;
	private String lSMOauthGraphURL = OAuthGraphURL;

	LSMOAuthHttpManager lsmOAuthHttpManager = new LSMOAuthHttpManager(OAuthGraphURL);

	public static LSMSecurityManagerService getInstance() {
		// if (instance == null)
		// instance = new LSMAbstractSecurityManager();
		return instance;
	}

	private LSMSecurityManagerService() {
	}

	public Permission getPermission(String perId) {
		return lsmOAuthHttpManager.getPermission(perId);
	}

	public void deletePermission(String perId) {
		lsmOAuthHttpManager.deletePermission(perId);
	}

	public void addPermission(Permission permission) {
		lsmOAuthHttpManager.addPermission(permission);

	}

	public Role getRole(String roleId) {
		return lsmOAuthHttpManager.getRole(roleId);
	}

	public void deleteRole(String roleId) {
		lsmOAuthHttpManager.deleteRole(roleId);
	}

	public void addRole(Role role) {
		lsmOAuthHttpManager.addRole(role);
	}

	public User getUser(String userId) {
		return lsmOAuthHttpManager.getUser(userId);
	}

	public void deleteUser(String userId) {
		lsmOAuthHttpManager.deleteUser(userId);
	}

	public void addUser(User user) {
		lsmOAuthHttpManager.addUser(user);
	}

	/**
	 * Retrieves a user by the username
	 * 
	 * @param username
	 * @return
	 */
	public User getUserByUsername(String username) {
		org.openiot.lsm.security.oauth.mgmt.User user = null;
		String userURL = "http://lsm.deri.ie/resource/user/" + username;
		if (username.contains("http://lsm.deri.ie/resource/user/")) {
			userURL = username;
			username = username.substring(username.lastIndexOf("/") + 1);
		}
		String sparql = " select ?nick ?mbox ?pass ?role" + " from <" + lSMOauthGraphURL + "> \n" + "where{ " + "<" + userURL
				+ "> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://openiot.eu/ontology/ns/User>." + "OPTIONAL{<" + userURL
				+ "> <http://xmlns.com/foaf/0.1/nick> ?nick.}" + "OPTIONAL{<" + userURL + "> <http://xmlns.com/foaf/0.1/mbox> ?mbox.}" + "<" + userURL
				+ "> <http://openiot.eu/ontology/ns/password> ?pass." + "<" + userURL + "> <http://openiot.eu/ontology/ns/role> ?role." + "}";
		try {
			String service = "http://lsm.deri.ie/sparql";
			QueryExecution vqe = new QueryEngineHTTP(service, sparql);
			ResultSet results = vqe.execSelect();
			user = new org.openiot.lsm.security.oauth.mgmt.User();
			user.setUsername(username);
			while (results.hasNext()) {
				QuerySolution soln = results.nextSolution();
				user.setEmail(soln.get("?mbox").toString());
				user.setPassword(soln.get("?pass").toString());
				user.setName(soln.get("?nick").toString());
				List<Role> roles = user.getRoles();
				if (roles == null) {
					roles = new ArrayList<Role>();
					user.setRoles(roles);
				}
				Role role = getRole(soln.get("?role").toString());
				if (!roles.contains(role)) {
					roles.add(role);
				}
			}
			vqe.close();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return user;
	}

	/**
	 * Retrieves a user by the email
	 * 
	 * @param email
	 * @return
	 */
	public User getUserByEmail(String email) {
		org.openiot.lsm.security.oauth.mgmt.User user = null;
		
		String sparql = " select ?userId" + " from <" + lSMOauthGraphURL + "> \n" + "where{ "
				+ " ?userId <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://openiot.eu/ontology/ns/User>."
				+ " ?userId <http://xmlns.com/foaf/0.1/mbox> \"" + email+ "\"}";
		try {
			String service = "http://lsm.deri.ie/sparql";
			QueryExecution vqe = new QueryEngineHTTP(service, sparql);
			ResultSet results = vqe.execSelect();
		
			while (results.hasNext()) {
				QuerySolution soln = results.nextSolution();
				user = getUser(soln.get("?userId").toString());
			}
			vqe.close();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		return user;
	}

	// /********************************
	// * To be retrieved from LSM *
	// ********************************/
	// public abstract User getUserById(Long userId);
	//
	// /********************************
	// * To be retrieved from LSM *
	// ********************************/
	// public abstract List<Role> getAllRoles();
	//
	// /********************************
	// * To be retrieved from LSM *
	// ********************************/
	// public abstract List<Permission> getAllPermissions();
	//
	// /********************************
	// * To be retrieved from LSM *
	// ********************************/
	// public abstract boolean addRoleToUser(User user, Role role);
	//
	// /********************************
	// * To be retrieved from LSM *
	// ********************************/
	// public abstract boolean addPermissionToRole(Role role, Permission permission, Long
	// serviceId);
	//
	// /********************************
	// * To be retrieved from LSM *
	// ********************************/
	// public abstract boolean saveUser(User user, char[] passwd);
	//
	// /********************************
	// * To be retrieved from LSM *
	// ********************************/
	// public abstract boolean savePermission(Permission permission);
	//
	// /********************************
	// * To be retrieved from LSM *
	// ********************************/
	// public abstract boolean saveRole(Role role);
}
