<?php

$ips = array_filter(array_map('trim', file('.htallowedips')));
$ip = @$_SERVER['HTTP_X_FORWARDED_FOR'] ? $_SERVER['HTTP_X_FORWARDED_FOR'] : $_SERVER['REMOTE_ADDR']; 
if (!in_array($ip, $ips)) {
    exit('Accès interdit (votre IP : '.$ip.')');
}

header('Content-type: text/html; charset=utf-8');

$data = array(
    'train' => array(
        'title' => 'Infos train',
        'files' => glob('cache/train-*'),
    ),
    'prochainsdeparts' => array(
        'title' => 'Prochains départs',
        'files' => glob('cache/gare-*'),
    ),
);
        
$do = isset($_GET['do']) ? $_GET['do'] : 'index';
$do_type = isset($_GET['type']) ? $_GET['type'] : null;

?>
<p>Adresse IP : <strong><?php echo $_SERVER['REMOTE_ADDR'] ?></strong> (<?php echo $_SERVER['HTTP_X_FORWARDED_FOR'] ?>)</p>
<h1>Statistiques</h1>
<ul>
    <?php foreach ($data as $type => $info): ?>
    <li><strong><?php echo count($info['files']) ?></strong> fichier(s) de cache pour "<?php echo $info['title'] ?>"</li>
    <?php endforeach ?>
</ul>
<p>[<a href="?do=list">Lister les fichiers</a>]<?php if ($do == 'list'): ?>[<a href="?do=clear" onclick="return confirm('Confirmer le vidage de tous les caches')">Vider le cache</a>]<?php endif ?></p>

<?php if ($do == 'list' || $do == 'clear'): ?>

    <?php foreach ($data as $type => $info): ?>
    <?php
    $files = array();
    $size = 0;
    foreach ($info['files'] as $path) {
        $files[] = array(
            'path' => $path,
            'size' => $s = filesize($path),
            'date' => filemtime($path),
        );
        $size += $s;
    }
    if (isset($_GET['sort']) && in_array($sort = $_GET['sort'], array('path', 'size', 'date'))) {
        $index = array();
        foreach ($files as $key => $row) {
            $index[$key] = $row[$sort];
        }
        array_multisort($index, SORT_ASC, $files);
    }
    $size  = round($size/1024, 2);
    ?>
    <h1><?php echo $info['title'] ?></h1>
    <?php if ($do == 'list' || ($do == 'clear' && $do_type != $type)): ?><p><a href="?do=clear&type=<?php echo $type ?>" onclick="return confirm('Confirmer le vidage de ce cache')">Vider ce cache (<?php echo $size ?> ko)</a>.</p><?php endif ?>
    <p id="l<?php echo $type ?>"><a href="#" onclick="document.getElementById('l<?php echo $type ?>').style.display='none';document.getElementById('t<?php echo $type ?>').style.display='';return false;">Afficher la liste</a></p>
    <table id="t<?php echo $type ?>" style="display:none">
        <thead>
            <tr>
                <th><a href="?do=list&sort=path">Fichier</a></th>
                <th><a href="?do=list&sort=size">Poids</a></th>
                <th><a href="?do=list&sort=date">Date</a></th>
                <th><?php echo ($do == 'list') ? 'Action' : 'Statut' ?></th>
            </tr>
        </thead>
        <tbody>
            <?php foreach ($files as $file): ?>
                <tr>
                    <td><?php echo $file['path'] ?></td>
                    <td><?php echo $file['size'] ?></td>
                    <td><?php echo date('d/m/Y H:i:s', $file['date']) ?></td>
                    <td>
                        <?php if ($do == 'list'): ?>
                            -
                        <?php elseif ($do == 'clear' && (is_null($do_type) || $do_type == $type)): ?>
                            <?php echo unlink($file['path']) ? 'Supprimé' : 'Erreur' ?>
                        <?php endif ?>
                    </td>
                </tr>
            <?php endforeach ?>
        </tbody>
    </table>
    <?php endforeach ?>

<?php endif ?>
