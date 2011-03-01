Welcome to the maven-diff-plugin plugin for Apache Maven 2.

## Available goals

 * diff:diff

## Getting started

To use this plugin declare the plugin:

    ....
    <plugins>
      <plugin>
        <groupId>org.ahedstrom.maven.plugin</groupId>
        <artifactId>maven-diff-plugin</artifactId>
        <version>0.0.4</version>
        <executions>
          <execution>
            <phase>initialize</phase>
            <goals>
              <goal>diff</goal>
            </goals>
          </execution>
         </executions>
         <configuration>
           <originalFiles>
             <originalFile>[uri to original file]</originalFile>					
           </originalFiles>
           <revisedFiles>
             <revisedFile>[uri to revised file to diff against]</revisedFile>					
           </revisedFiles>
           <abortBuildOnDiff>true</abortBuildOnDiff>
         </configuration>
      </plugin>
    </plugins>
    ....
    
### Dependencies

		<dependency>
			<groupId>com.googlecode.java-diff-utils</groupId>
			<artifactId>diffutils</artifactId>
			<version>1.2</version>
		</dependency>
