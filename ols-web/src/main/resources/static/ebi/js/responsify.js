/**
 * Copyright (c) EMBL-EBI 2016
 */
(function responsify(){var requireScripts=[];var requireStyles=[];var i,j;function isMatch(path,includes,excludes){var match=false;for(var i=0;i<includes.length&&!match;i++){pattern=new RegExp(includes[i],'');if(pattern.test(path)){match=true;}}
for(var i=0;i<excludes.length&match;i++){pattern=new RegExp(excludes[i],'');if(pattern.test(path)){match=false;}}
return match;}
function init(){try{var existingStyles=document.getElementsByTagName('link');var gotStyle,putStyle;for(j=0;j<requireStyles.length;j++){for(gotStyle=false,i=0;i<existingStyles.length;i++)
if(existingStyles[i].href.indexOf(requireStyles[j])!==-1)
gotStyle=true;if(!gotStyle){putComment=document.createComment(requireStyles[j]+' automatically inserted');putStyle=document.createElement('link');putStyle.type='text/css';putStyle.rel='stylesheet';putStyle.media='screen';putStyle.href=requireStyles[j];document.body.appendChild(putComment);document.body.appendChild(putStyle);}}
var existingScripts=document.getElementsByTagName('script');var gotScript,putScript;for(j=0;j<requireScripts.length;j++){for(gotScript=false,i=0;i<existingScripts.length;i++)
if(existingScripts[i].src.indexOf(requireScripts[j])!==-1)
gotScript=true;if(!gotScript){putComment=document.createComment(requireScripts[j]+' automatically inserted');putScript=document.createElement('script');putScript.type='text/javascript';putScript.src=requireScripts[j];document.body.appendChild(putComment);document.body.appendChild(putScript);}}}
catch(err){console.log(err);setTimeout(init,500);}}
var includePaths=['^/$','^/about','^/services','^/research','^/training','^/industry','^/support','^/GOA'];var excludePaths=['^/support/ipd\.php$','^/support/hla\.php$'];var includeClasses=['environment-prod','environment-stage','environment-dev'];var excludeClasses=[];if(isMatch(document.body.className,includeClasses,excludeClasses)){if(!window.jQuery){var script=document.createElement('script');script.type="text/javascript";script.src="//www.ebi.ac.uk/misc/jquery.js?v=1.4.4";document.getElementsByTagName('head')[0].appendChild(script);}
if(isMatch(document.location.pathname,includePaths,excludePaths)){requireScripts.push('//www.ebi.ac.uk/web_guidelines/responsify/responsify-analytics.js');if(/Android|webOS|iPhone|iPad|iPod|BlackBerry|BB10|IEMobile|Silk|Nokia|Opera Mini/i.test(navigator.userAgent)){document.body.style.paddingTop='1000px';requireScripts.push('//www.ebi.ac.uk/web_guidelines/responsify/responsify-page-specific.js');requireStyles.push('//www.ebi.ac.uk/web_guidelines/responsify/responsify-page-specific.css');requireScripts.push('//www.ebi.ac.uk/web_guidelines/responsify/responsify-global.js');requireStyles.push('//www.ebi.ac.uk/web_guidelines/responsify/responsify-global.css');requireScripts.push('//www.ebi.ac.uk/web_guidelines/responsify/responsify-content.js');requireStyles.push('//www.ebi.ac.uk/web_guidelines/responsify/responsify-content.css');}}}
init();})();