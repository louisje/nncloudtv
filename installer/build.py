import os, datetime, shutil

#---- get environment ----  
choice = raw_input('Environment (1. dev 2.prod/stage) : ')
server="prod"
if choice == "1":
   server = "dev" 

#---- copy property files ----
list=['datanucleus_analytics.properties', 'datanucleus_content.properties', 'datanucleus_billing.properties', 'clearcommerce.properties',
      'datanucleus_nnuser1.properties', 'datanucleus_nnuser2.properties', 'datanucleus_recommend.properties', 'aws.properties',
      'memcache.properties', 'queue.properties', 'facebook.properties', 'crawler.properties', 'services.properties', 'googleplay.properties',
      'appstore.properties', 'youtube.properties']

for l in list:
   src = server + "/" + l
   dst = "../src/main/resources/" + l
   shutil.copyfile(src, dst)

if server == "dev":
   print "copy file completed. exit"
   exit()

#---- write version info ----
version = raw_input('Enter version number : ')
os.system("git log --pretty=format:\"%H\" -n 1 > version")
source = open("version", "rU")
for line in source:
   rev =  line.rstrip()
   break
source.close
print "Revision number:" + rev
now = datetime.datetime.utcnow()
print now

old_file = open("..//src//main//java//com//nncloudtv//web//VersionController.java", "rU")
new_file = open("..//src//main//java//com//nncloudtv//web//VersionController.java.tmp",'w')
for line in old_file:
  if (line.find("String appVersion") > 0):
     line = "        String appVersion = \"" + version + "\";\n"
  if (line.find("String hash") > 0):
     line = "        String hash = \"" + rev + "\";\n"
  if (line.find("String packagedTime") > 0):
     line = "        String packagedTime = \"" + str(now) + "\";\n"
     
  new_file.write(line)
old_file.close()
new_file.close()

os.remove("..//src//main//java//com//nncloudtv//web//VersionController.java")
os.rename("..//src//main//java//com//nncloudtv//web//VersionController.java.tmp", "..//src//main//java//com//nncloudtv//web//VersionController.java")

#---- build ----
os.chdir("..//")
os.system("mvn clean")
os.system("mvn install:install-file -Dfile=./lib/CcxClientApi.jar -DgroupId=com.clearcommerce -DartifactId=clear-commerce -Dversion=5.10.550.5432 -Dpackaging=jar")
os.system("mvn compile")
os.system("mvn datanucleus:enhance")
os.system("mvn war:war")

#---- summary ----
print "\n--- summary ---\n"
print "Package environment:" + server
print "Version number:" + version
print "Revision number:" + rev

