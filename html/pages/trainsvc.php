	<div id="content">
<!--
Main Body of the page
-->
		<div id="page">
			<h2>Train an SVM Classification Model</h2>
			<p align="justify">



<?php echo "<form enctype=\"x-www-form-urlencoded\" method=\"POST\" action=\"".$svctrainurl."\">"; ?>
<br/>
Dataset to train the model<br/>
<input type="text" name="dataId" value="35"/> 
<br/><br/>
Cost<br/>
<input type="text" name="cost" value="1000"/><br/>
<br/>
SVM kernel<br/>
<select name="kernel" onchange="setOptions(this.value)" id="kernel">
<option >RBF</option>
<option >Sigmoid</option>
<option >Polynomial</option>
<option >Linear</option>
</select>
<br/><br/>

<div id="Options">
</div>
<br/><br/>
<input type="submit" value="Train Model" />
</form>

</p>

<!--
End of Main Body
-->
		</div>

