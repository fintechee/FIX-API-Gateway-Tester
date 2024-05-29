package org.fixapi.testgateway;

import java.util.Scanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import quickfix.Application;
import quickfix.FieldNotFound;
import quickfix.IncorrectTagValue;
import quickfix.Message;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionSettings;
import quickfix.UnsupportedMessageType;
import quickfix.field.Account;
import quickfix.field.ClOrdID;
import quickfix.field.OrdType;
import quickfix.field.OrderQty;
import quickfix.field.OrigClOrdID;
import quickfix.field.Price;
import quickfix.field.Side;
import quickfix.field.Symbol;
import quickfix.field.TimeInForce;
import quickfix.field.TransactTime;
import quickfix.fix44.ExecutionReport;
import quickfix.fix44.Logon;
import quickfix.fix44.MessageCracker;
import quickfix.fix44.NewOrderSingle;
import quickfix.fix44.OrderCancelRequest;

public class Order extends MessageCracker implements Application {
	private static final Logger logger = LoggerFactory.getLogger(Order.class);

	private Session defaultSession;

	public Order() {
	}

	public void fromAdmin(Message message, SessionID sessionID) {
		if (message instanceof Logon) {
			logger.warn("fromAdmin!!!!!!!!!!");
		}
	}

	public void fromApp(Message message, SessionID sessionID) {
		try {
			crack(message, sessionID);
		} catch (Exception e) {
			e.printStackTrace();
			logger.error(e.getMessage());
		}
	}

	public void toAdmin(Message message, SessionID sessionID) {
		if (message instanceof Logon) {
			logger.warn("toAdmin!!!!!!!!!!");
		}
	}

	public void toApp(Message message, SessionID sessionID) {
	}

	public void onCreate(SessionID sessionID) {
		logger.warn("onCreate!!!!!!!!!!");
	}

	public void onLogon(SessionID sessionID) {
		logger.warn("Order via onLogon login begun");
		defaultSession = Session.lookupSession(sessionID);
	}

	public void onLogout(SessionID sessionID) {
		logger.warn("Order logged out via onLogout");
	}

	private boolean isLoggedOn() {
        return defaultSession != null && defaultSession.isLoggedOn();
    }

	public void logout() {
		if (defaultSession != null) {
			defaultSession.logout();
		}
	}

	public void sendNewOrderSingle(String accountId, String symbolName, double orderQty, String orderType, String type, double price) {
        if (isLoggedOn()) {
    		try {
    			NewOrderSingle order = new NewOrderSingle();

    			order.set(new ClOrdID(System.currentTimeMillis() + ""));
    			order.set(new Symbol(symbolName));

    			if ("BUY".equals(orderType)) {
    				order.set(new Side(Side.BUY));
    			} else {
    				order.set(new Side(Side.SELL));
    			}

    			order.set(new OrderQty(orderQty));

    			order.set(new Price(price));
    			if (price == 0) {
    				order.set(new OrdType(OrdType.MARKET));
    				order.set(new TimeInForce(TimeInForce.FILL_OR_KILL));
    			} else {
    				if ("LIMIT".equals(type)) {
    					order.set(new OrdType(OrdType.LIMIT));
    				} else {
    					order.set(new OrdType(OrdType.STOP_STOP_LOSS));
    				}
    				order.set(new TimeInForce(TimeInForce.GOOD_TILL_CANCEL));
    			}

    			order.set(new TransactTime());

    			if (!"null".equals(accountId)) {
        			order.setString(Account.FIELD, accountId);
    			}

    			defaultSession.send(order);
    		} catch (Exception e) {
    			e.printStackTrace();
    			logger.error(e.getMessage());
    		}
        }
	}

	public void sendOrderCancel(String accountId, String originalClOrderId) {
        if (isLoggedOn()) {
    		try {
    			OrderCancelRequest ocr = new OrderCancelRequest();

    			ocr.set(new ClOrdID(System.currentTimeMillis() + ""));
    			ocr.set(new OrigClOrdID(originalClOrderId));
    			ocr.set(new Side(Side.BUY));
    			ocr.set(new TransactTime());

    			if (!"null".equals(accountId)) {
    				ocr.setString(Account.FIELD, accountId);
    			}

    			defaultSession.send(ocr);
    		} catch (Exception e) {
    			e.printStackTrace();
    			logger.error(e.getMessage());
    		}
        }
	}

    @Override
    public void onMessage(ExecutionReport executionReport, SessionID sessionId) throws FieldNotFound, UnsupportedMessageType, IncorrectTagValue {
		ClOrdID clOrdID = new ClOrdID();
		executionReport.get(clOrdID);
		String clOrderId = clOrdID.getValue();
		logger.info("Your OrigClOrdID for OrderCancelRequest: " + clOrderId);
    }
}
