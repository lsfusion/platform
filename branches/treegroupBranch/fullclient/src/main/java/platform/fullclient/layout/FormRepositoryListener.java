package platform.fullclient.layout;

public interface FormRepositoryListener {
	/**
	 * Invoked when a picture was added to the observed repository.
	 * @param picture the new picture
	 */
	public void pictureAdded(int picture);

	/**
	 * Invoked when a picture was removed from the observed repository.
	 * @param picture the removed picture
	 */
	public void pictureRemoved(int picture);
}
