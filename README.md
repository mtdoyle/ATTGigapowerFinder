# ATT Gigapower Speed Finder using PhantomJS headless browser

Instructions on how to set an environment up to run this program:

* Clone this repository
* Download/install Vagrant, Ansible and VirtualBox
  * Vagrant: https://www.vagrantup.com/downloads.html
  * Ansible: http://docs.ansible.com/ansible/intro_installation.html
    * note on Ansible install - it's very simple if you use pip for python to install it
  * VirtualBox: https://www.virtualbox.org/wiki/Downloads
* Modify the Vagrantfile at Resources/vagrant/Vagrantfile and tweak the CPU and RAM. The defaults are fine, but if you don't have enough memory/CPU cores, you might want to lower them
* Navigate to Resources/vagrant and type: "vagrant up" without quotes
  * This should kick off the automated installation process of everything. Depending on your internet connection speed and CPU specs, it could be anywhere from a few minutes to a few hours.
  

Once your Vagrant vm is running and the Ansible script is done (with no errors, hopefully):
* type "vagrant ssh" to remote into your Vagrant machine that was just created
* navigate to the clspeed directory
* load addresses into rabbitmq:
  *"python load_rabbitmq.py addresses"
  
What the vagrant script does is gives you a local instance to load your addresses into (RabbitMQ) and store the results (mysql). When you run the script, point the variables in local.properties to your vagrant instance. You can run this script on multiple machines that all point to the same vagrant instance - the benefit of this is that RabbitMQ hands out addresses to instances of the script requesting them and the results are stored on the Vagrant's mysql instance. Once all addresses have been analyzed, you'll have a mysql table with all of your data.

To see the results: 
* log into phpmyadmin by navigating to http://localhost:8180/phpmyadmin on your machine running the VM (not the actual VM itself)
  * username: gigapower
  * password: gigapower
* Once logged in, expand the gigapower item on the left-hand panel, then click on gigapower_{YEAR}\_{MONTH}\_{DAY}
![clspeed table image](http://i.imgur.com/mRC5Pck.jpg)
  * Note: the image above was from my centurylink script - mentally replace "clspeed" with "gigapower"
* This should open up the table with the results. 

To export the gigabit addresses to a CSV, do the following:
* Click on the SQL tab at the top of the screen
* In the SQL box, type:
```SELECT * FROM `gigapower_2016_05_11` WHERE speed = '1000'```
  * Be sure to change the table name in the sql above to your actual table name
* Click the "Go" button
* You should now be back at your table page but only showing addresses with a speed value of 1000.0
* In the "Query results operations" box, click on Export
* In the Export screen: 
  * Choose "Custom - display all possible options"
  * Select "CSV" from the Format drop-down menu (NOT CSV for MS Excel)
  * Check the box that says "Put columns names in the first row"
  * Click "Go" button - this will download your CSV
  
To generate a map:
* Open your CSV in a spreadsheet program. 
  *I have used MS Excel, LibreOffice Calc and Google Sheets for this part. Any spreadsheet program should work.
  *Be sure to tell the spreadsheet program to use comma delimited columns when opening your CSV
* Select all rows/columns in the spreadsheet (cmd+a / ctrl+a) and copy (cmd+c / ctrl+c)
* Navigate to http://www.easymapmaker.com/
* In the box at top that says "Click here to paste data", click in it then paste your data
* Since we're only mapping one speed (1000), we can ignore having multiple colors for markers
* Unselect "enable clustering" 
* Underneath the box where you pasted your data, click on the "Make Map" button
* You should now see a map with all your points on it. 
* Click on the "Launch Map Save" button below
  * Give it a title of some sort
  * Make it public if you wish to share the map with other people
* Click "Save Map"
* You'll be brought back to the previous page, and it will show you the URL for your new map.
  * I highly suggest you click on the "Edit" button next to the URL and give it a URL which is easier to remember 
  
That's it - you can share the URL to your map

