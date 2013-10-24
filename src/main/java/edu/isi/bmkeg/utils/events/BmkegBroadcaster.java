package edu.isi.bmkeg.utils.events;

import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.flex.messaging.MessageTemplate;

import flex.messaging.MessageBroker;
import flex.messaging.messages.AsyncMessage;
import flex.messaging.services.MessageService;
import flex.messaging.util.UUIDUtils;

public class BmkegBroadcaster {

	private MessageTemplate template;
	
	@Autowired
	public void setTemplate(MessageTemplate template) {
		this.template = template;
	}

	public void broadcast(String whaddyaWannaSay) {
		
		MessageBroker mb = this.template.getMessageBroker();
		MessageService ms = (MessageService) mb.getService("message-service");
				
		AsyncMessage msg = new AsyncMessage();
        msg.setDestination( "bmkegBroadcast");
        msg.setMessageId(UUIDUtils.createUUID());
        msg.setBody(whaddyaWannaSay);
    	
        // Checking in to see how many people are receiving this messsage.
		//Set s = ms.getSubscriberIds(msg, false);	
        
        ms.pushMessageToClients(msg, false);

	}
	
	
}
