package edu.isi.bmkeg.utils.events;

import flex.messaging.messages.AsyncMessage;
import flex.messaging.messages.Message;
import flex.messaging.services.MessageService;
import flex.messaging.services.ServiceAdapter;
import flex.messaging.util.UUIDUtils;

/**
 * Test service adapter.  Great for testing when you want to JUST SEND AN OBJECT and nothing
 * else.  This class has to stay in the main codebase (instead of test) because, when 
 * it's used it needs to be deployed to Tomcat.
 * @author Kevin G
 * [see http://stackoverflow.com/questions/1872742/
 * 		how-to-push-data-from-blazeds-without-recieve-message-from-flex-client]
 *
 */
public class BmkegServiceAdapter extends ServiceAdapter {

	    private boolean running;
	    private int count = 0;

	    private Message createTestMessage() {
	        Integer objectToSend = new Integer(count);
	        count++;
	        
	        final AsyncMessage msg = new AsyncMessage();
	        msg.setDestination( "bmkegBroadcast");
	        msg.setClientId(UUIDUtils.createUUID());
	        msg.setMessageId(UUIDUtils.createUUID());
	        msg.setBody(objectToSend);

	        return msg;
	    }

	    private void sendMessageToClients(Message msg) {
	    	
	    	MessageService ms = (MessageService) getDestination().getService();
	    	ms.pushMessageToClients(msg, false);
	    	
	    }

	    /**
	     * @see flex.messaging.services.ServiceAdapter#start()
	     */
	    @Override
	    public void start(){    
	        super.start();

	        Thread messageSender = new Thread(){
	            public void run(){
	                running = true;
	                while(running){
	                	
	                    try {
							sendMessageToClients(createTestMessage());
						} catch (Exception e) {
							e.printStackTrace();
						}
	                    secondsToSleep(3);
	                }
	            }
	        };

	        messageSender.start();        
	    }
	    /**
	     * @see flex.messaging.services.ServiceAdapter#stop()
	     */
	    @Override
	    public void stop(){
	        super.stop();
	        running = false;
	    }
	    
	    /**
	     * This method is called when a producer sends a message to the destination.
	     *  Currently,we don't care when that happens.
	     */
	    @Override
	    public Object invoke(Message message) {
	        if (message.getBody().equals("stop")) {
	            running = false;
	        }
	        return null;
	    }
	    
	    private void secondsToSleep(int seconds) {
	        try{
	            Thread.sleep(seconds * 1000);
	        }catch(InterruptedException e){
	            System.out.println("TestServiceAdapter Interrupted while sending messages");
	            e.printStackTrace();
	        }
	    }    
	    
	}