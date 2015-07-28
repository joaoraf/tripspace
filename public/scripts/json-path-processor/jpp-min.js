!function(f){if("object"==typeof exports&&"undefined"!=typeof module)module.exports=f();else if("function"==typeof define&&define.amd)define([],f);else{var g;g="undefined"!=typeof window?window:"undefined"!=typeof global?global:"undefined"!=typeof self?self:this,g.JsonPathProcessor=f()}}(function(){return function e(t,n,r){function s(o,u){if(!n[o]){if(!t[o]){var a="function"==typeof require&&require;if(!u&&a)return a(o,!0);if(i)return i(o,!0);var f=new Error("Cannot find module '"+o+"'");throw f.code="MODULE_NOT_FOUND",f}var l=n[o]={exports:{}};t[o][0].call(l.exports,function(e){var n=t[o][1][e];return s(n?n:e)},l,l.exports,e,t,n,r)}return n[o].exports}for(var i="function"==typeof require&&require,o=0;o<r.length;o++)s(r[o]);return s}({1:[function(require,module,exports){"use strict";function JPP(data){this._data=data}var parsePath=function(path){var R=[];return path?path.match?path.match(/\[|\]/)?(""===!("."+path).replace(/\.([^\.\[]*)|\[\'([^\]]+)\'\]/g,function(M,D,A){return R.push(void 0===D?A:D),""}),R.reverse()):path.split(/\./).reverse():[path]:[]},jsonpath=function(obj,path,assign,create,del){var key,P=parsePath(path),OO=obj?obj:create?{}:null,O=obj;if(null===OO&&!create)return void 0;for(;P.length;){switch(key=P.pop()){case"$":case"":continue}if(void 0!==OO[key]&&null!==OO[key])OO=OO[key];else{if(void 0===create)return void 0;P.length&&(OO[key]={}),OO=OO[key]}1===P.length&&(O=OO)}if(del)return Array.isArray(O)?O.splice(key,1):delete O[key],OO;if(void 0!==assign)try{key?O[key]=assign.call?assign(OO):assign:O=assign.call?assign(OO):assign}catch(E){create&&key&&key&&(O[key]=create)}return OO};JPP.prototype={value:function(path){return this._data&&path?jsonpath(this._data,path):this._data},get:function(path){return new JPP(this.value(path))},set:function(path,value,create,del){return path&&"$"!==path?(!create||null!==this._data&&"object"==typeof this._data||(this._data={}),jsonpath(this._data,path,value,create,del)):jsonpath(this,"_data",value,create,del),this},copy:function(from,to,skip){return this.set(to,this.value(from),skip?void 0:null)},del:function(path){return this.set(path,void 0,!1,!0)},move:function(from,to){var V=this.value(from);return void 0!==V&&(this.set(to,V,!0),this.del(from)),this},each:function(path,cb,elsecb){var V=this.value(path);return V?Array.isArray(V)?this.set(path,V.map(function(V,I){var R;try{return R=cb(V,I),void 0===R?V:R}catch(E){return V}})):this:elsecb?this.set(path,elsecb,!0):this},forIn:function(path,cb,elsecb){var V=this.value(path),R={};return V?"object"==typeof V?(Object.keys(V).map(function(D){try{R[D]=cb(V[D],D)}catch(E){R[D]=V[D]}}),this.set(path,R)):this:elsecb?this.set(path,elsecb,!0):this},filter:function(path,cb,elsecb){var R,V=this.value(path);return V?Array.isArray(V)?this.set(path,V.filter(function(V,I){try{return cb(V,I)}catch(E){return!0}})):"object"==typeof V?(R={},Object.keys(V).map(function(D){try{cb(V[D],D)&&(R[D]=V[D])}catch(E){R[D]=V[D]}}),this.set(path,R)):this:elsecb?this.set(path,elsecb,!0):this},find:function(path,cb){var I,V=this.value(path);if(V)for(I in V)try{if(cb(V[I]))return V[I]}catch(E){}},findLast:function(path,cb){var I,R,V=this.value(path);if(V){for(I in V)try{cb(V[I])&&(R=V[I])}catch(E){}return R}},range:function(path,a1,a2,a3){var I,R=[],args=[a1];void 0!==a2&&args.push(a2),args.length<2&&args.unshift(0),void 0===a3&&(a3=1);try{for(I=args[0];I<args[1];I+=a3)R.push(I)}catch(E){}return this.set(path,R,[])},concat:function(){var args=Array.prototype.slice.call(arguments),all=[];return args.map(function(P){var V=this.value(P);Array.isArray(V)&&(all=all.concat(V))},this),all.length&&this.set(arguments[0],all,!0),this}},module.exports=function(data,path){return path?jsonpath(data,path):new JPP(data)},module.exports.parsePath=parsePath},{}]},{},[1])(1)});