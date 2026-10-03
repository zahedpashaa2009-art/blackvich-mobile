(function(){
  if(window.__bvShell) return; window.__bvShell=true;
  var A=window.Android, path=location.pathname.replace(/\/$/,"")||"/order";
  if(path==="") path="/order";

  /* فونت داخل برنامه (اگر فایلش در assets/fonts باشد) */
  var st=document.createElement("style");
  st.textContent=
   '@font-face{font-family:"Vazirmatn";src:url("https://appassets.local/fonts/Vazirmatn-Variable.woff2") format("woff2");font-weight:100 900;font-display:swap}'+
   'body{padding-bottom:calc(78px + env(safe-area-inset-bottom,0px))!important;-webkit-user-select:none;user-select:none}'+
   'nav a[href="/order"],nav a[href="/admin"],a[href^="/order"],a[href^="/admin"]{display:none!important}'+
   'input,textarea{-webkit-user-select:text;user-select:text}'+
   '#bv-dock{position:fixed;left:50%;bottom:calc(12px + env(safe-area-inset-bottom,0px));transform:translateX(-50%) translateY(0);z-index:99999;display:flex;gap:4px;padding:6px;border-radius:26px;'+
   'background:linear-gradient(160deg,rgba(255,246,232,.16),rgba(255,246,232,.06));border:1px solid rgba(255,246,232,.2);'+
   'backdrop-filter:blur(24px) saturate(1.5);-webkit-backdrop-filter:blur(24px) saturate(1.5);box-shadow:0 18px 44px #000b,inset 0 1px 0 #fff5;'+
   'animation:bvUp .7s cubic-bezier(.2,.9,.2,1) both;direction:rtl;transition:transform .35s}'+
   '#bv-dock.hide{transform:translateX(-50%) translateY(140%)}'+
   '@keyframes bvUp{from{opacity:0;transform:translateX(-50%) translateY(60px) scale(.9)}to{opacity:1;transform:translateX(-50%)}}'+
   '#bv-dock a,#bv-dock button{position:relative;display:flex;flex-direction:column;align-items:center;gap:3px;min-width:96px;padding:9px 10px;border:0;border-radius:20px;'+
   'background:none;color:#c9b696;font:700 11px Vazirmatn,Tahoma,sans-serif;text-decoration:none;transition:.25s}'+
   '#bv-dock svg{width:22px;height:22px;stroke:currentColor;fill:none;stroke-width:1.9;stroke-linecap:round;stroke-linejoin:round;transition:.3s}'+
   '#bv-dock a:active,#bv-dock button:active{transform:scale(.92)}'+
   '#bv-dock a.on{color:#fff;background:linear-gradient(135deg,#ff8a3d,#f5401f 55%,#c4121c);box-shadow:0 8px 22px #f5401f66}'+
   '#bv-dock a.on svg{transform:translateY(-1px) scale(1.12)}'+
   '#bv-dock .dc{color:#ff9a8a}'+
   '#bv-modal{position:fixed;inset:0;z-index:100000;display:flex;align-items:center;justify-content:center;background:#000a;backdrop-filter:blur(6px);-webkit-backdrop-filter:blur(6px);animation:bvF .25s both;direction:rtl;padding:24px}'+
   '@keyframes bvF{from{opacity:0}}@keyframes bvP{from{opacity:0;transform:scale(.88) translateY(20px)}}'+
   '#bv-modal div.b{width:100%;max-width:340px;padding:22px;border-radius:28px;text-align:center;background:linear-gradient(160deg,rgba(255,246,232,.16),rgba(255,246,232,.05));'+
   'border:1px solid rgba(255,246,232,.2);box-shadow:0 24px 60px #000c,inset 0 1px 0 #fff4;animation:bvP .4s cubic-bezier(.2,1.2,.4,1) both;color:#f6e3bd;font-family:Vazirmatn,Tahoma,sans-serif}'+
   '#bv-modal h4{margin:0 0 6px;font-size:18px}#bv-modal p{margin:0 0 18px;font-size:13px;color:#c9b696;line-height:1.9}'+
   '#bv-modal .r{display:flex;gap:10px}#bv-modal button{flex:1;padding:13px;border:0;border-radius:16px;font:800 14px Vazirmatn,Tahoma,sans-serif;color:#fff;background:rgba(255,246,232,.12)}'+
   '#bv-modal button.d{background:linear-gradient(135deg,#ff8a3d,#f5401f 55%,#c4121c);box-shadow:0 8px 22px #f5401f55}';
  document.head.appendChild(st);

  var ic={
   order:'<svg viewBox="0 0 24 24"><path d="M6 2 3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"/><path d="M3 6h18M16 10a4 4 0 0 1-8 0"/></svg>',
   kitchen:'<svg viewBox="0 0 24 24"><path d="M6 13.9V20h12v-6.1M3 11a4 4 0 0 1 3-3.9A4 4 0 0 1 12 4a4 4 0 0 1 6 3.1 4 4 0 0 1 3 3.9z"/></svg>',
   admin:'<svg viewBox="0 0 24 24"><path d="M3 3v18h18"/><path d="M7 15l4-5 3 3 5-7"/></svg>',
   out:'<svg viewBox="0 0 24 24"><path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5M21 12H9"/></svg>'};
  var items=[]; // فقط آشپزخانه: منوی صفحات دیگر نداریم
  var d=document.createElement("nav"); d.id="bv-dock";
  items.forEach(function(it){
    var a=document.createElement("a"); a.href=it[0]; a.innerHTML=it[2]+"<span>"+it[1]+"</span>";
    if(path.indexOf(it[0])===0||(it[0]==="/order"&&path==="/")) a.classList.add("on");
    a.addEventListener("click",function(){try{A.haptic(12)}catch(e){}});
    d.appendChild(a);
  });
  var o=document.createElement("button"); o.className="dc"; o.innerHTML=ic.out+"<span>قطع اتصال</span>";
  o.onclick=function(){modal("قطع اتصال از کامپیوتر؟","برای وصل شدن دوباره باید لینک یا QR را وارد کنی.","قطع اتصال",function(){A&&A.disconnect()})};
  d.appendChild(o);
  document.body.appendChild(d);

  /* با اسکرول به پایین، نوار پنهان می‌شود */
  var ly=0; window.addEventListener("scroll",function(){var y=window.scrollY;
    if(y>ly+8&&y>120)d.classList.add("hide"); else if(y<ly-8)d.classList.remove("hide"); ly=y},{passive:true});
  /* هنگام تایپ (کیبورد باز) نوار پنهان شود */
  document.addEventListener("focusin",function(e){if(/INPUT|TEXTAREA|SELECT/.test(e.target.tagName))d.classList.add("hide")});
  document.addEventListener("focusout",function(){setTimeout(function(){d.classList.remove("hide")},200)});

  function modal(t,p,yes,fn){
    var m=document.createElement("div"); m.id="bv-modal";
    m.innerHTML='<div class="b"><h4>'+t+'</h4><p>'+p+'</p><div class="r"><button class="n">ادامه می‌دهم</button><button class="d">'+yes+'</button></div></div>';
    m.querySelector(".n").onclick=function(){m.remove()};
    m.querySelector(".d").onclick=function(){m.remove();fn()};
    m.addEventListener("click",function(e){if(e.target===m)m.remove()});
    document.body.appendChild(m);
  }
  window.__bvConfirmExit=function(){ if(!document.getElementById("bv-modal")) modal("خروج از برنامه؟","اتصال به کامپیوتر ذخیره می‌ماند و دفعه‌ی بعد خودکار وصل می‌شود.","خروج",function(){A&&A.exit()}) };
})();
