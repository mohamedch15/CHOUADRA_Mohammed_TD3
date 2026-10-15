package fr.uha.miage.plateforme.jeux.shifumi;

enum Coup {
    PIERRE,
    FEUILLE,
    CISEAUX;

    boolean bat(Coup autre) {
        return (this == PIERRE && autre == CISEAUX)
                || (this == FEUILLE && autre == PIERRE)
                || (this == CISEAUX && autre == FEUILLE);
    }
}
