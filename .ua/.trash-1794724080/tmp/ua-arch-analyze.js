const fs=require('fs');
try{
const d=JSON.parse(fs.readFileSync(process.argv[2],'utf8'));
const nodes=d.fileNodes, imp=d.importEdges||[], all=d.allEdges||[];
const P={routes:'api',api:'api',controllers:'api',handlers:'api',services:'service',core:'service',lib:'service',domain:'service',models:'data',model:'data',db:'data',data:'data',dao:'data',database:'data',ui:'ui',screens:'ui',components:'ui',views:'ui',utils:'utility',util:'utility',helpers:'utility',config:'config',theme:'ui',factory:'service',docs:'documentation',assets:'assets',sql:'data',schema:'data'};
const rel=p=>p.startsWith('src/main/java/btd/')?p.slice(18):p;
const grp=n=>{const r=rel(n.filePath||'');const s=r.split('/');if(n.filePath.startsWith('src/main/java/btd/')){if(s.length===1)return 'root-src';if(s[0]==='model'||s[0]==='ui')return s.length>2?s[0]+'/'+s[1]:s[0];return s[0];}return s.length>1?s[0]:'root';};
const G={},T={},g={};nodes.forEach(n=>{const k=grp(n);g[n.id]=k;(G[k]=G[k]||[]).push(n.id);(T[n.type]=T[n.type]||[]).push(n.id);});
const fi={},fo={},ig={},intra={};
imp.forEach(e=>{fo[e.source]=(fo[e.source]||0)+1;fi[e.target]=(fi[e.target]||0)+1;const a=g[e.source],b=g[e.target];if(!a||!b)return;intra[a]=intra[a]||{internalEdges:0,totalEdges:0};intra[b]=intra[b]||{internalEdges:0,totalEdges:0};if(a===b){intra[a].internalEdges++;intra[a].totalEdges++;}else{intra[a].totalEdges++;intra[b].totalEdges++;const k=a+'->'+b;ig[k]=(ig[k]||0)+1;}});
Object.values(intra).forEach(v=>v.density=v.totalEdges?+(v.internalEdges/v.totalEdges).toFixed(2):0);
const typ={};nodes.forEach(n=>typ[n.id]=n.type);const cc={};all.forEach(e=>{const a=typ[e.source],b=typ[e.target];if(!a||!b)return;const k=a+'|'+b+'|'+e.type;cc[k]=(cc[k]||0)+1;});
const inter=Object.entries(ig).map(([k,c])=>{const[a,b]=k.split('->');return{from:a,to:b,count:c}});
const dir=[];inter.forEach(x=>{const r=ig[x.to+'->'+x.from]||0;if(x.count>r||(x.count===r&&x.from<x.to))dir.push({dependent:x.from,dependsOn:x.to});});
const pm={};Object.keys(G).forEach(k=>{const last=k.split('/').pop();pm[k]=P[last]||P[k.split('/')[0]]||null;});
const fp=nodes.map(n=>n.filePath);
const out={scriptCompleted:true,directoryGroups:G,nodeTypeGroups:T,crossCategoryEdges:Object.entries(cc).map(([k,c])=>{const[a,b,t]=k.split('|');return{fromType:a,toType:b,edgeType:t,count:c}}),interGroupImports:inter,intraGroupDensity:intra,patternMatches:pm,
deploymentTopology:{hasDockerfile:fp.some(p=>/Dockerfile/.test(p)),hasCompose:fp.some(p=>/docker-compose/.test(p)),hasK8s:false,hasTerraform:fp.some(p=>/\.tf$/.test(p)),hasCI:fp.some(p=>/\.github\/workflows/.test(p)),infraFiles:fp.filter(p=>/Dockerfile|docker-compose|\.tf$|\.github|Makefile/.test(p))},
dataPipeline:{schemaFiles:fp.filter(p=>/\.sql$/.test(p)),migrationFiles:fp.filter(p=>/migrations\//.test(p)),dataModelFiles:fp.filter(p=>/\/model\//.test(p)),apiHandlerFiles:[]},
docCoverage:(()=>{const ks=Object.keys(G);const w=ks.filter(k=>G[k].some(id=>typ[id]==='document'));return{groupsWithDocs:w.length,totalGroups:ks.length,coverageRatio:+(w.length/ks.length).toFixed(2),undocumentedGroups:ks.filter(k=>!w.includes(k))}})(),
dependencyDirection:dir,fileStats:{totalFileNodes:nodes.length,filesPerGroup:Object.fromEntries(Object.entries(G).map(([k,v])=>[k,v.length])),nodeTypeCounts:Object.fromEntries(Object.entries(T).map(([k,v])=>[k,v.length]))},fileFanIn:fi,fileFanOut:fo};
fs.writeFileSync(process.argv[3],JSON.stringify(out,null,2));}catch(e){console.error(e);process.exit(1);}
