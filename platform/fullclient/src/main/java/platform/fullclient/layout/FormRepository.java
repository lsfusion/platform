package platform.fullclient.layout;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FormRepository {
	/** observers of this repository, will be informed whenever pictures are added or removed */
	private List<FormRepositoryListener> listeners = new ArrayList<FormRepositoryListener>();
	
	/** the pictures in this repository*/
	private List<Form> pictures = new ArrayList<Form>();
	
	/**
	 * Writes the pictures of this repository into <code>out</code>.
	 * @param out the stream to write into
	 * @throws IOException if an I/O error occurs
	 */
	public void write( DataOutputStream out ) throws IOException{
	    out.writeInt( pictures.size() );
	    for( Form picture : pictures )
	        picture.write( out );
	}


	/**
	 * Reads the pictures of this repository from <code>in</code>.
	 * @param in the stream to read from
	 * @throws IOException if an I/O error occurs
	 */
	public void read( DataInputStream in ) throws IOException{
	    while( !pictures.isEmpty() )
	        remove( pictures.get( pictures.size()-1 ) );

	    for( int i = 0, n = in.readInt(); i<n; i++ ){
	        Form picture = new Form();
	        picture.read( in );
	        add( picture );
	    }
	}

	
	/**
	 * Adds a picture to the list of pictures.
	 * @param picture the new picture
	 */
	public void add( Form picture ){
		pictures.add( picture );
		//for( FormRepositoryListener listener : listeners.toArray( new FormRepositoryListener[ listeners.size() ] ) )
	//		listener.pictureAdded( picture );
	}
	
	/**
	 * Removes a picture from the list of pictures.
	 * @param picture the picture to remove
	 */
	public void remove( Form picture ){
		if( pictures.remove( picture )){
			for( FormRepositoryListener listener : listeners.toArray( new FormRepositoryListener[ listeners.size() ] ) )
				listener.pictureRemoved( picture );	
		}
	}
	

	public Form getPicture( String name ){
		for( Form picture : pictures )
			if( picture.getName().equals( name ))
				return picture;
		
		return null;
	}

}
