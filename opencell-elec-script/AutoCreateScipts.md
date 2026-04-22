<p>To upload scripts automatically to a running Opencell use the following command : 
<code>mvn opencell:deploy-scripts@deploy-scripts</code>
you can specify a different Opencell URL:
<code>mvn opencell:deploy-scripts@deploy-scripts -Dopencell.url=<a href="http://integration.i.opencellsoft.com">http://integration.i.opencellsoft.com</a></code></p>

<p>To export scripts to a Postman file for a later use, use the following command : 
<code>mvn opencell:create-postman@create-postman</code></p>

<p>See <a href="https://opencell.assembla.com/spaces/meveo-enterprise/git-6/source">Opencell plugin</a> for more information.</p>

###How to use 
<p>run <code> cd opencell-elec-scripts</code> </p>
<p>run <code> mvn opencell:deploy-scripts@deploy-scripts -P deploy-script</code></p>
<p>run <code> mvn opencell:create-postman@create-postman -P deploy-script</code></p> 