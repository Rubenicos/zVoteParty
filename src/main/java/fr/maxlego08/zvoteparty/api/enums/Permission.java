package fr.maxlego08.zvoteparty.api.enums;

public enum Permission {
	
	
	ZVOTEPARTY_USE,
	ZVOTEPARTY_RELOAD,
	ZVOTEPARTY_CONFIG,
	ZVOTEPARTY_ADD,
	ZVOTEPARTY_STARTPARTY,
	ZVOTEPARTY_HELP,
	ZVOTEPARTY_VOTE, ZVOTEPARTY_REMOVE,

	;

	private String permission;

	private Permission() {
		this.permission = this.name().toLowerCase().replace("_", ".");
	}

	public String getPermission() {
		return permission;
	}

}
