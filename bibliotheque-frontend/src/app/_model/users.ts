export class Users {
    userId: number;
    username: string;
    name: string;
    password: string;
    // Valeur par défaut nécessaire : le formulaire lit/écrit role[0].roleName
    // dès le premier rendu (avant tout appel au service), donc role ne peut
    // jamais rester undefined sans faire planter la détection de changements.
    role: any[] = [{ roleName: 'User' }];
}
