package org.ajoberstar.gradle.git.auth;

import org.eclipse.jgit.transport.JschConfigSessionFactory;
import org.eclipse.jgit.transport.OpenSshConfig.Host;
import org.eclipse.jgit.util.FS;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.agentproxy.AgentProxyException;
import com.jcraft.jsch.agentproxy.Connector;
import com.jcraft.jsch.agentproxy.RemoteIdentityRepository;
import com.jcraft.jsch.agentproxy.USocketFactory;
import com.jcraft.jsch.agentproxy.connector.PageantConnector;
import com.jcraft.jsch.agentproxy.connector.SSHAgentConnector;
import com.jcraft.jsch.agentproxy.usocket.JNAUSocketFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A session factory that supports use of ssh-agent and Pageant SSH authentication.
 * @since 0.6.0
 */
public class JschAgentProxySessionFactory extends JschConfigSessionFactory {
	private static final Logger logger = LoggerFactory.getLogger(JschAgentProxySessionFactory.class);

	/**
	 * No actions performed by this.
	 */
	protected void configure(Host hc, Session session) {
		// no action
	}

	/**
	 * Obtains a JSch used for creating sessions, with the addition
	 * of ssh-agent and Pageant agents, if available.
	 * @return the JSch instance
	 */
	@Override
	protected JSch getJSch(Host hc, FS fs) throws JSchException {
		JSch jsch = super.getJSch(hc, fs);
		Connector con = determineConnector();
		if (con != null) {
			jsch.setIdentityRepository(new RemoteIdentityRepository(con));
		}
		return jsch;
	}

	/**
	 * Chooses which agent proxy connector is used.
	 * @return the connector available at this time
	 */
	private Connector determineConnector() {
		try {
			if (SSHAgentConnector.isConnectorAvailable()) {
				logger.info("ssh-agent available");
				USocketFactory usf = new JNAUSocketFactory();
				return new SSHAgentConnector(usf);
			} else if (PageantConnector.isConnectorAvailable()) {
				logger.info("pageant available");
				return new PageantConnector();
			} else {
				logger.info("jsch agent proxy not available");
				return null;
			}
		} catch (AgentProxyException e) {
			logger.debug("Could not configure JSCH agent proxy connector.", e);
			return null;
		}
	}
}
