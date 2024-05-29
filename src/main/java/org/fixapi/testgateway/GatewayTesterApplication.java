package org.fixapi.testgateway;

import java.io.InputStream;
import java.util.Scanner;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import quickfix.DefaultMessageFactory;
import quickfix.FileLogFactory;
import quickfix.FileStoreFactory;
import quickfix.LogFactory;
import quickfix.MessageFactory;
import quickfix.MessageStoreFactory;
import quickfix.SessionSettings;
import quickfix.SocketInitiator;

@SpringBootApplication
public class GatewayTesterApplication implements CommandLineRunner {
	private static final Logger logger = LoggerFactory.getLogger(GatewayTesterApplication.class);

	private static String orderCfgFileName = "order.cfg";

	private static SocketInitiator orderInitiator = null;
	private static Order orderFIX = null;

	public static void main(String[] args) {
		logger.info("Opening......");

		startOrderFix();
		SpringApplication.run(GatewayTesterApplication.class, args);
	}

	@PreDestroy
	public void onExit() {
		logger.info("Closing......");

		try {
			endOrderFix();
			Thread.sleep(5 * 1000);
		} catch (InterruptedException e) {
			logger.error(e.getMessage());
		}
	}

	private static boolean endOrderFix() {
		try {
			if (orderInitiator != null) {
				orderInitiator.stop(true);
				orderInitiator = null;
				orderFIX = null;
				return true;
			}
			return false;
		} catch (Exception e) {
			orderInitiator = null;
			orderFIX = null;
			e.printStackTrace();
			logger.error(e.getMessage());
			return false;
		}
	}

	private static boolean startOrderFix() {
		if (orderInitiator != null) {
			orderInitiator.stop(true);
			try {
				logger.warn("Sending Logon!!!!!!!!!!");
				orderInitiator.start();
				return true;
			} catch (Exception e) {
				orderInitiator = null;
				e.printStackTrace();
				logger.error(e.getMessage());
				return false;
			}
		}

		boolean result = false;

		InputStream fileInputStream = null;

		try {
			fileInputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(orderCfgFileName);
			SessionSettings settings = new SessionSettings(fileInputStream);
			fileInputStream.close();
			fileInputStream = null;

			orderFIX = new Order();
			MessageStoreFactory storeFactory = new FileStoreFactory(settings);
			LogFactory logFactory = new FileLogFactory(settings);
			MessageFactory messageFactory = new DefaultMessageFactory();
			orderInitiator = new SocketInitiator(orderFIX, storeFactory, settings, logFactory,
					messageFactory);

			logger.warn("Sending Logon!!!!!!!!!!");
			orderInitiator.start();

			result = true;
		} catch (Exception e) {
			orderInitiator = null;
			e.printStackTrace();
			logger.error(e.getMessage());
		} finally {
			if (fileInputStream != null) {
				try {
					fileInputStream.close();
				} catch (Exception e) {
					e.printStackTrace();
					logger.error(e.getMessage());
				}
			}
		}

		return result;
	}

	@Override
	public void run(String... args) throws Exception {
        Scanner scanner = new Scanner(System.in);

        while (true) {
            System.out.println("Enter command（press enter to run the command， input exit to close it）：");
            System.out.println("Command format:NewOrderSingle,Account,Symbol,OrderQty,Side,OrdType,Price");
            System.out.println("For example:NewOrderSingle,null,EUR/USD,1000,BUY,MARKET,0");
            System.out.println("Command format:OrderCancelRequest,Account,OrigClOrdID");
            System.out.println("For example:OrderCancelRequest,null,xxxxx-check-your-log-to-get-it-xxxxx");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("exit")) {
                System.out.println("Exit!");
                break;
            }

            executeCommand(input);
        }

        orderFIX.logout();
        scanner.close();
        System.exit(0);
    }

    private void executeCommand(String command) {
        System.out.println("Running: " + command);
        String[] commands = command.split(",");
        String message = commands[0];
        if ("NewOrderSingle".equals(message)) {
            String accountId = commands[1];
            String symbolName = commands[2];
            double orderQty = Double.parseDouble(commands[3]);
            String orderType = commands[4];
            String type = commands[5];
            double price = Double.parseDouble(commands[6]);

            orderFIX.sendNewOrderSingle(accountId, symbolName, orderQty, orderType, type, price);
        } else {
            String accountId = commands[1];
            String originalClOrderId = commands[2];

            orderFIX.sendOrderCancel(accountId, originalClOrderId);
        }
    }
}
