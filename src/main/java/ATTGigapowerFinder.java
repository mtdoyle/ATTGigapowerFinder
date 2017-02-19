import com.rabbitmq.client.*;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class ATTGigapowerFinder implements Runnable {
    String maxSpeed;
    String address;
    Connection conn;
    Channel channel;
    long deliveryTag;
    String currDate;
    LoadProperties loadProperties = LoadProperties.getInstance();
    Properties properties = loadProperties.getProperties();
    String street = "";
    String city = "";
    String state = "";
    String zip = "";
    String lat = "";
    String lon = "";
    String emmAcc = "";

    public ATTGigapowerFinder(Connection conn, String currDate) throws IOException {
        this.conn = conn;
        this.currDate = currDate;
    }

    public ATTGigapowerFinder() throws IOException, TimeoutException {
        this.conn = getConnectionFactory();
    }

    private void setUpConnection() throws IOException {
        channel = conn.createChannel();

        channel.basicQos(1);

        channel.queueDeclare("gigapower", true, false, false, null);

        GetResponse response = channel.basicGet("gigapower", false);
        if (response == null) {
            //no message received
        } else {
            AMQP.BasicProperties props = response.getProps();
            byte[] body = response.getBody();
            this.deliveryTag = response.getEnvelope().getDeliveryTag();
            this.address = new String(body, "UTF-8");
        }
    }

    public void run() {
        try {
            setUpConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            checkAddress();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        try {
            channel.basicAck(deliveryTag, false);
            channel.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }
    }

    private Connection getConnectionFactory() throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername(properties.getProperty("rabbitmqUsername"));
        factory.setPassword(properties.getProperty("rabbitmqPassword"));
        factory.setVirtualHost(properties.getProperty("rabbitmqVirtualHost"));
        factory.setHost(properties.getProperty("rabbitmqServer"));
        factory.setPort(Integer.valueOf(properties.getProperty("rabbitmqPort")));
        return factory.newConnection();
    }

    public void checkAddress () throws InterruptedException {
        String[] choppedAddress = address.split(",");
        String submitAddress;
        if (choppedAddress.length == 7) {
            street = choppedAddress[0];
            city = choppedAddress[1];
            state = choppedAddress[2];
            zip = choppedAddress[3];
            lat = choppedAddress[4];
            lon = choppedAddress[5];
            emmAcc = choppedAddress[6];

            submitAddress = String.format("%s, %s",
                    street,
                    zip);
        } else {
            street = choppedAddress[0];
            city = choppedAddress[1];
            zip = choppedAddress[2].split(" ")[1];
            lat = choppedAddress[4];
            lon = choppedAddress[5];
            emmAcc = choppedAddress[6];

            submitAddress = String.format("%s, %s",
                    street,
                    zip);
        }
        DesiredCapabilities caps = new DesiredCapabilities();
        caps.setJavascriptEnabled(true);
        caps.setCapability(PhantomJSDriverService.PHANTOMJS_PAGE_SETTINGS_PREFIX + "loadImages", false);
        PhantomJSDriver webdriver = new PhantomJSDriver(caps);
        Logger.getLogger(PhantomJSDriverService.class.getName()).setLevel(Level.OFF);
        webdriver.manage().window().setSize(new Dimension(1400,1000));

        webdriver.get("https://www.att.com/shop/unified/availability.html");
        webdriver.findElement(By.id("streetaddress")).sendKeys(street);
        webdriver.findElement(By.id("zipcode")).sendKeys(zip);
        webdriver.findElement(By.xpath("//*[@id=\"content\"]/div/div[2]/div[1]/div/div/div/form/div[2]/input")).click();

        long millis = System.currentTimeMillis();
        long currTime = millis;
        while (currTime - millis < 10000) {
            if (webdriver.findElements(By.xpath("//*[@id=\"content\"]/div/div[1]/div[5]/div[1]/div/div/div[2]/div/span/p")).size() > 0){
                break;
            }
            else {
                currTime = System.currentTimeMillis();
            }
        }

        if (webdriver.findElements(By.xpath("//*[@id=\"content\"]/div/div[1]/div[5]/div[1]/div/div/div[2]/div/span/p")).size() > 0){
            writeSpeedToDB("1000", currDate);
        } else if (webdriver.getPageSource().contains("Mbps")){
            if (webdriver.getPageSource().contains("'Select the services you’re interested in'")) {
                WebElement element = webdriver.findElement(By.xpath("//*[@id=\"offerTilesDiv\"]/div[1]/div[1]/div/div[5]/div[2]/div[2]/p[1]/span[2]"));

            }
            writeSpeedToDB("1000", currDate);
        }

    }
    private void writeSpeedToDB(String speed, String currDate){
        String sql = String.format("insert into %s_%s " +
                        "(street, city, state, zip, speed, emm_lat, emm_lng, emm_acc)" +
                        "values ('%s', '%s', '%s', '%s', %s, %s, %s, '%s')",
                properties.getProperty("databaseTableName"),
                currDate, street, city, state.trim(), zip, speed, lat, lon, emmAcc);
        WriteToMySQL writeToMySQL = new WriteToMySQL();
        writeToMySQL.executeStatement(sql);
    }

}

