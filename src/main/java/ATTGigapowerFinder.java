import com.rabbitmq.client.*;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.phantomjs.PhantomJSDriver;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

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

        street = choppedAddress[0];
        city = choppedAddress[1].trim();
        state = choppedAddress[2].trim();
        zip = choppedAddress[3];
        lat = choppedAddress[6];
        lon = choppedAddress[7];
        emmAcc = choppedAddress[5];

        PhantomJSDriver webdriver = new PhantomJSDriver();

        webdriver.get("https://www.att.com/shop/unified/availability.html");

        long millis = System.currentTimeMillis();
        long currTime = millis;
        while (currTime - millis < 10000) {
            if (webdriver.getPageSource().contains("Check availability")) {
                break;
            }
            currTime = System.currentTimeMillis();
        }

        webdriver.findElement(By.id("streetaddress")).sendKeys(street);
        webdriver.findElement(By.id("zipcode")).sendKeys(zip);
        webdriver.findElement(By.xpath("//*[@id=\"content\"]/div/div[2]/div[1]/div/div/div/form/div[2]/input")).click();

        millis = System.currentTimeMillis();
        currTime = millis;
        while (currTime - millis < 10000) {
            if (webdriver.getPageSource().contains("These services are available")) {
                break;
            }
            currTime = System.currentTimeMillis();
        }

        WebElement element;
        if (webdriver.getPageSource().contains("These services are available")) {
            if (webdriver.findElements(By.xpath("/html/body/div[5]/div[1]/div/section/div/div/div[2]/div/div/div[1]/div[5]/div[1]/div/div[2]/div[2]/div/span/div/div[2]/p[1]/span")).size() > 0) {
                element = webdriver.findElement(By.xpath("/html/body/div[5]/div[1]/div/section/div/div/div[2]/div/div/div[1]/div[5]/div[1]/div/div[2]/div[2]/div/span/div/div[2]/p[1]/span"));
                maxSpeed = element.getAttribute("innerHTML").split(" ")[3].replace("Mbps", "");
                writeSpeedToDB(maxSpeed, currDate);
            } else if (webdriver.findElements(By.xpath("/html/body/div[5]/div[1]/div/section/div/div/div[2]/div/div/div[1]/div[5]/div[1]/div/div/div[2]/div/span/div/div[2]/p[1]/span")).size() > 0) {
                element = webdriver.findElement(By.xpath("/html/body/div[5]/div[1]/div/section/div/div/div[2]/div/div/div[1]/div[5]/div[1]/div/div/div[2]/div/span/div/div[2]/p[1]/span"));
                maxSpeed = element.getAttribute("innerHTML").split(" ")[3].replace("Mbps", "");
                writeSpeedToDB(maxSpeed, currDate);
            }
        }

        webdriver.close();
    }
    private void writeSpeedToDB(String speed, String currDate){
        String sql = String.format("insert into %s_%s " +
                        "(street, city, state, zip, speed, emm_lat, emm_lng, emm_acc)" +
                        "values (\"%s\", \"%s\", \"%s\", \"%s\", %s, %s, %s, \"%s\")",
                properties.getProperty("databaseTableName"),
                currDate, street, city, state.trim(), zip, speed, lat, lon, emmAcc);
        WriteToMySQL writeToMySQL = new WriteToMySQL();
        writeToMySQL.executeStatement(sql);
    }

}

