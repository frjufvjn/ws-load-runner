package com.hansol.mail;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Launcher;
import io.vertx.core.Promise;
import io.vertx.ext.mail.LoginOption;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;

public class MailClientVerticle extends AbstractVerticle {

	public static void main(String[] args) {
		// verticle launcher
		Launcher.executeCommand("run", new String[]{MailClientVerticle.class.getName()});
	}

	@Override
	public void start(Promise<Void> startPromise) throws Exception {
		MailConfig mailConfig = new MailConfig()
				.setHostname("echomail4.hansol.com")
				// .setPort(5870)
				.setLogin(LoginOption.REQUIRED)
				.setAuthMethods("PLAIN")
				.setUsername("087816")
				.setPassword("flvorxhfld79!");

		MailClient mailClient = MailClient.createShared(vertx, mailConfig);
		
	}
}
