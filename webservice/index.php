<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<?php
$pages = array(
  'home' => 'Le projet',
  'app'  => 'L\'application',
  'data' => 'Les données',
  'ws'   => 'Le webservice',
);

$page = null;
if (isset($_GET['p'])) {
  $page = $_GET['p'];
}
if (is_null($page)) {
  $page = 'home';
}
?>
<html>

<head>
  <meta http-equiv="Content-Type" content="text/html; charset=utf-8">
  <title>Web-Service gares TER SNCF</title>
  <link rel="stylesheet" type="text/css" href="style.css" media="screen" />
</head>

<body>
  <div id="page">
    <div id="header">
      <h1><a href="#">Gares TER SNCF</a></h1>
      <div class="description">Web Service et application Android</div>
    </div>
    <div id="mainarea">
      <div id="sidebar">
        <div id="sidebarnav">
          <?php foreach ($pages as $p => $t): ?>
          <a<?php if ($page == $p): ?> class="active" <?php endif ?> href="?p=<?php echo $p ?>"><?php echo $t ?></a>
          <?php endforeach ?>
        </div>
      </div>
      
      <div id="contentarea">

<h2><?php echo isset($pages[$page]) ? $pages[$page] : 'Page introuvable...' ?></h2>

<?php if ($page == 'home'): ?>
<h3>La genèse</h3>
<p><strong>Le constat</strong> : la SNCF ne se bouge pas le fion pour Android, il faut croire qu'on n'existe pas, 
alors que l'iPhone (solution ultra-propriétaire rappelons-le) a lui une application de qualité... Alors les amateurs 
réagissent, et créent des applications qui vont chercher les données où elles peuvent, c'est-à-dire en exploitant les 
sites SNCF.</p>
<p><strong>L'offre amateur</strong> : la plupart des applications qui ont été mises à la disposition du public par les
amateurs sont basées sur le site <a href="http://www.gares-en-mouvement.com">Gares en mouvement</a>. OK c'est pas mal,
et ça a le mérite d'être rapide, mais ça ne représente que 160 gares sur les 3000 de France :(</p>
<p>Il existe un site mobile signé par la SNCF pour les gares TER ! <a href="http://www.termobile.fr">TER Mobile</a> est
certes <em>théoriquement</em> adapté aux écrans de mobile, il n'est pas du tout adapté à l'utilisation nomade... Avant
d'accéder aux horaires de sa gare il faut passer par l'accueil (obligatoirement, impossible de mettre une page en
favori direct), cliquer sur "prochains départs", taper le nom de sa gare, sélectionner la gare exacte dans la page
suivante, et enfin valider pour voir les horaires. Ces étapes, avec un clavier de mobile et une connexion hors 3G peut
facilement prendre plusieurs minutes ! Cependant, ce service existe, et j'ai donc décidé de créer une application qui se
base dessus...</p>
<h3>La première version</h3>
<p>J'ai réalisé une application au design sobre, et aussi fiable que je puisse faire avec mes maigres connaissances du
développement Android à ce moment-là. L'objectif dès le départ est le suivant : <strong>Pouvoir accéder à la liste des
départs de la gare proche de chez moi en un seul clic !</strong> Cette objectif nécessitant de se baser sur la 
géolocalisation, et de pouvoir interroger en une étape le site TERMobile.</p>
<p>J'ai récupéré la liste des gares voyageurs de France, via le site officiel du Réseau Ferré de France. J'ai géolocalisé
chacune de ces gares grâce à Google Maps, afin d'obtenir une base de données (précieuse !) de 3081 gares de France
localisées.</p>
<p>On sélectionne sa gare parmi les plus proches grâce à la localisation, et lorsqu'on clique sur une gare l'application 
va :</p>
<ul>
  <li>Accéder à l'accueil de termobile.fr pour récupérer le cookie de session.</li>
  <li>Aller sur la page de recherche, et valider le formulaire avec le nom de la gare choisie.</li>
  <li>Sélectionner automatiquement la gare s'il n'y a qu'un résultat, sinon demander à l'utilisateur de confirmer parmi la
  liste des gares trouvées par le site.</li>
  <li>Confirmer pour accéder à la page des prochains départs, et "parser" le HTML pour en sortir une liste affichée à 
  l'utilisateur.</li>
</ul>
<h3>L'échec</h3>
<p>Cette étape d'interrogation est trop instable, le cookie n'est pas toujours correctement gardée, et les 4 requêtes
d'affilé prennent un temps aberrant en edge... Beaucoup d'utilisateurs rapportent des erreurs (le site qui renvoie une
erreur 500, voire des pages vides, à cause d'une connexion trop erratique), et l'application a une note assez pitoyable
de 2.5/5.</p>
<p>Cette note quoique cruelle est méritée car l'application est certes pratique, elle ne marche que rarement... La faute
à quoi ? Plusieurs éléments : la connexion instable avec le site termobile.fr, les sockets Java disponibles dans l'API
Android qui ne fonctionnent pas aussi bien que cURL sous PHP pour gérer les redirections avec conservation de cookie
par exemple, et aussi le HTML qui bouge trop souvent, nécessitant une mise à jour à chaque fois.</p>
<h3>La lumière au bout du tunnel</h3>
<p>Je n'abandonne pas, et comprends que la solution doit passer par un <em>proxy</em> entre l'application et le site
termobile.fr, sous la forme d'un service web, qui me permette de récupérer les mêmes données mais :</p>
<ul>
  <li>De manière compacte (JSON par exemple)</li>
  <li>En une seule requête, et sans avoir besoin de cookie</li>
</ul>
<p>Une fois ces critères réunis, l'application est enfin stabilisée au niveau de sa connexion. De plus le "parsing" du
HTML est déportée sur le serveur, ce qui permet d'être bien plus réactif en cas de modification de la mise en page. Sans
compter que le webservice peut implémenter un cache de requête ce qui permettra, si plusieurs personnes cherchent des 
infos simultanement sur la même gare, de ne pas surcharger le serveur.</p>
<?php endif ?>

<?php if ($page == 'app'): ?>
<h3>
  <img src="http://cache.cyrket.com/p/android/com.naholyr.android.horairessncf/icon" alt="Horaires TER SNCF" />
  Horaires TER SNCF
</h3>
<p>
  <img src="app/device1.png" width="250" alt="Accueil : liste des gares proches" />
  <img src="app/device2.png" width="250" alt="Prochains départs : liste des prochains départs de Car/Train/TGV depuis la gare sélectionnée" />
</p>
<p>
  Installation (Android Market) :<br />
  <a href="market://search?q=pname:com.naholyr.android.horairessncf"><img src="http://chart.apis.google.com/chart?cht=qr&chs=200x200&chl=market://search?q=pname:com.naholyr.android.horairessncf" /></a>
</p>
<p>Fiche Cyrket : <a href="http://www.cyrket.com/p/android/com.naholyr.android.horairessncf/">com.naholyr.android.horairessncf</a>.</p>
<?php endif ?>

<?php if ($page == 'data'): ?>
<p>Vous pouvez télécharger l'ensemble des 3081 gares de France ici, il s'agit d'un fichier au format texte brut, encodé
en UTF8. Le fichier n'est pas forcément 100% fiable car la localisation a été effectuée via Google Maps à partir du nom
seul des gares + leur région. En cas d'erreur, n'hésitez pas à m'envoyer un mail à <code>naholyr[AT]gmail[POINT]com</code>
pour que je puisse le corriger.</p>
<p>Format (1 ligne = 1 gare) :<br /><code>Nom de la gare#Adresse de la gare#Latitude#Longitude</code></p>
<p>Exemple :<br /><code>Paris-Gare-de-Lyon#Ile de France#Gare de Lyon, 75012 Paris, France#48.8447174#2.3739303</code></p>
<p align="center" style="font-size:2em"><a href="data/gares.utf8.txt">Téléchargement (mise à jour : 30/04/10)</a></p>
<?php endif ?>

<?php if ($page == 'ws'): ?>
<p>Format général de retour :</p>
<ul>
  <li>Succès : <code>{code:2XX, success:...}</code></li>
  <li>Erreur : <code>{code:3XX ou 4XX, error:"...", info:...}</code></li>
</ul>
<h3>Prochains départs</h3>
<p>Adresse du service : <code>http://termobile-ws.sfhost.net/prochainsdeparts.php</code> (<a href="prochainsdeparts.php?source=1">voir la source</a>)</p>
<h4 id="recherche-gare">Recherche de gare</h4>
<ul>
  <li>Appel : <code>?gare=Nom-De-La-Gare</code></li>
  <li>Réponse standard : <code>code=201, success={gares=Liste des gares format Nom-De-La-Gare=Id-De-La-Gare}</code></li>
  <li>Réponse raccourcie : Si une seule gare est remontée par le serveur, alors on effectue directement la recherche des 
  prochains départs, et la réponse sera donc au format "<a href="#prochains-departs">Listing des prochains départs</a>".</li>
  <li>Exemple : <code>?gare=Villefranche</code> &rarr; <code>{"code":201,"success":{"gares":{"villefranche-d''alb.-ce":1750,"villefranche de rgue-12":1749,...}}}</code></li>
  <li>Codes d'erreur :
    <ul>
      <li><code>310 - Paramètre "gare" attendu</code> : Mauvaise utilisation du service qui attend au moins un paramètre</li>
      <li><code>320 - Erreur serveur (cookie de session)</code> : Il a été impossible de récupérer le cookie de session sur
      le site termobile.fr</li>
      <li><code>321 - Erreur serveur (status = X)</code> : Erreur serveur inattendue lors de la récupération des gares.</li>
      <li><code>330 - Aucune gare trouvee</code> : Aucune gare n'a été trouvée avec le nom demandé.</li>
      <li><code>340 - Erreur inconnue</code> : La réponse du serveur n'a pas pu être analysée, et il a été impossible d'en
      extraire une liste de gares, probable symptome d'une modification du format sur termobile.fr.</li>
      <li><code>331 - Aucune gare trouvee (bis)</code> : Après une tentative d'analyse de la réponse du serveur, aucune
      gare n'a pu être extraite, probable symptome d'une modification du format sur termobile.fr.</li>
    </ul>
  </li>
</ul>
<h4 id="prochains-departs">Listing des prochains départs</h4>
<ul>
  <li>Appel : <code>?gare=Nom-De-La-Gare&id=Id-De-La-Gare</li>
  <li>Réponse : <code>code=202, success={nom=Nom-Gare, id=Id-Gare, departs=Liste des departs}</code>,
  <br /> Format des départs : <code>type=Type-Train, numero=Numero-Train, heure=Heure-Depart, destination=Gare-Destination, retards=Liste des retards</code>,
  <br />Format des retards : <code>retard=Durée-Retard, motif=Justification-Retard</code>.
  <li>Exemple : <code>?gare=Villefranche-sur-Saone&id=6358</code> &rarr; <code></code></li>
  <li>Codes d'erreur :
    <ul>
      <li><code>322 - Erreur serveur (status = X)</code> : Erreur serveur inattendue lors de la récupération des gares.</li>
      <li><code>350 - ID invalide</code> : Identifiant de la gare invalide, aucune gare avec le nom recherché n'a été trouvée
      avec cet identifiant.</li>
      <li><code>351 - ID invalide (bis)</code> : Idem.</li>
    </ul>
  </li>
</ul>
<h3>Informations du train</h3>
<p>Adresse du service : <code>http://termobile-ws.sfhost.net/train.php</code> (<a href="train.php?source=1">voir la source</a>)</p>
<h4 id="recherche-gare">Infos sur le train</h4>
<ul>
  <li>Appel : <code>?num=Numéro-Du-Train</code></li>
  <li>Réponse standard : <code>code=200, success={numero=Numéro du train, date=Date, heures=Liste des horaires connus format Nom-Gare=Heure, arrets=Liste des gares dans l'ordre}</code></li>
  <li>Exemple : <code>?num=891501</code> &rarr; <code>{"code":200,"success":{"numero":"891501","date":"08 mai 2010","heures":{"Dijon-Ville":"20h33","Lyon-Perrache":"22h51"},"arrets":["Dijon-Ville","Beaune",...,"Lyon-Perrache"]}}</code></li>
  <li>Codes d'erreur :
    <ul>
      <li><code>310 - Paramètre "num" attendu</code> : Mauvaise utilisation du service qui attend au moins un paramètre</li>
      <li><code>311 - Paramètre "num" invalide</code> : Mauvais format pour le paramètre "num"</li>
      <li><code>320 - Erreur serveur (cookie de session)</code> : Il a été impossible de récupérer le cookie de session sur
      le site termobile.fr</li>
      <li><code>321 - Erreur serveur (status = X)</code> : Erreur serveur inattendue lors de la récupération des informations</li>
      <li><code>330 - Mauvais format de la réponse</code> : La réponse serveur est dans un format inattendu et non exploitable.</li>
    </ul>
  </li>
</ul>
<h3>Mirroring du service web</h3>
<p>Il peut être prévu - si le besoin s'en fait sentir - la mise en place un système de "mirrorring" afin que ceux qui souhaitent 
aider puissent héberger le script et déclarer qu'à telle adresse une copie du service est disponible. L'application se connectera
alors indifféremment à l'un des services déclarés pour récupérer les gares. C'est pourquoi j'ai laissé en libre consultation la
source de ce script (très basique vous le verrez).</p>
<?php endif ?>

      </div>
    </div>
    
    <div id="footer">
      Designed by <a href="http://www.free-css-templates.com/">Free CSS Templates</a>, Thanks to <a href="http://www.openwebdesign.org/">Custom Web Design</a>
    </div>
  </div>
</body>

</html>
