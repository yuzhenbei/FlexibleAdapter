package eu.davidea.examples.flexibleadapter;

import android.animation.Animator;
import android.content.Context;
import android.graphics.Typeface;
import android.support.v7.widget.RecyclerView;
import android.text.Html;
import android.text.Spannable;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import eu.davidea.examples.fastscroller.FastScroller;
import eu.davidea.flexibleadapter.FlexibleAnimatorAdapter;
import eu.davidea.flexibleadapter.FlexibleViewHolder;
import eu.davidea.flexibleadapter.FlexibleViewHolder.OnListItemClickListener;
import eu.davidea.utils.Utils;


public class ExampleAdapter extends FlexibleAnimatorAdapter<FlexibleViewHolder, Item>
		implements FastScroller.BubbleTextGetter {

	private static final String TAG = ExampleAdapter.class.getSimpleName();

	private Context mContext;
	private static final int
			EXAMPLE_VIEW_TYPE = 0,
			ROW_VIEW_TYPE = 1;

	private LayoutInflater mInflater;
	private OnListItemClickListener mClickListener;
	private RecyclerView mRecyclerView;

	//Selection fields
	private boolean
			mLastItemInActionMode = false,
			mSelectAll = false;

	public ExampleAdapter(Object activity, String listId, RecyclerView recyclerView) {
		super(DatabaseService.getInstance().getListById(listId), activity, recyclerView);
		this.mContext = (Context) activity;
		this.mClickListener = (OnListItemClickListener) activity;
		this.mRecyclerView = recyclerView;
		if (!isEmpty()) addUserLearnedSelection();
	}

	/**
	 * Param, in this example, is not used.
	 *
	 * @param param A custom parameter to filter the type of the DataSet
	 */
	@Override
	public void updateDataSet(String param) {
		//Refresh the original content
		mItems = DatabaseService.getInstance().getListById(param);

		if (!super.isEmpty()) addUserLearnedSelection();

		//Fill and Filter mItems with your custom list
		//Note: In case of userLearnSelection mItems is pre-initialized and after filtered.
		filterItems(mItems);
		notifyDataSetChanged();

		//Update Empty View
		mUpdateListener.onUpdateEmptyView(mItems.size());
	}

	private void addUserLearnedSelection() {
		if (!DatabaseService.userLearnedSelection && !hasSearchText()) {
			//Define Example View
			Item item = new Item();
			item.setId(0);
			item.setTitle(mContext.getString(R.string.uls_title));
			item.setSubtitle(mContext.getString(R.string.uls_subtitle));
			mItems.add(0, item);
		}
	}

	private void removeUserLearnedSelection() {
		if (!DatabaseService.userLearnedSelection && isEmpty()) {
			mItems.remove(0);
			notifyItemRemoved(0);
		}
	}

	private void userLearnedSelection() {
		//TODO: Save the boolean into Settings!
		DatabaseService.userLearnedSelection = true;
		mItems.remove(0);
		notifyItemRemoved(0);
	}

	@Override
	public void setMode(int mode) {
		super.setMode(mode);
		if (mode == MODE_SINGLE) mLastItemInActionMode = true;
	}

	@Override
	public void selectAll() {
		mSelectAll = true;
		super.selectAll(EXAMPLE_VIEW_TYPE);
	}

	@Override
	public void addItem(int position, Item item) {
		if (isEmpty()) {
			addUserLearnedSelection();
			notifyItemInserted(0);
		}
		super.addItem(position, item);
	}

	@Override
	public void removeItems(List<Integer> selectedPositions) {
		super.removeItems(selectedPositions);
		removeUserLearnedSelection();
	}

	@Override
	public boolean isEmpty() {
		return !DatabaseService.userLearnedSelection && mItems.size() == 1 || super.isEmpty();
	}

	@Override
	public int getItemViewType(int position) {
		return (position == 0 && !DatabaseService.userLearnedSelection
				&& !hasSearchText() ? EXAMPLE_VIEW_TYPE : ROW_VIEW_TYPE);
	}

	@Override
	public FlexibleViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		if (DEBUG) Log.d(TAG, "onCreateViewHolder for viewType " + viewType);
		if (mInflater == null) {
			mInflater = LayoutInflater.from(parent.getContext());
		}
		switch (viewType) {
			case EXAMPLE_VIEW_TYPE:
				return new ExampleViewHolder(
						mInflater.inflate(R.layout.recycler_uls_row, parent, false),
						this);
			default:
				return new ViewHolder(
						mInflater.inflate(R.layout.recycler_row, parent, false),
						this);
		}//end-switch
	}

	@Override
	public void onBindViewHolder(FlexibleViewHolder holder, final int position) {
		if (DEBUG) Log.d(TAG, "onBindViewHolder for position " + position);
		final Item item = getItem(position);

		//NOTE: ViewType Must be checked ALSO here to bind the correct view
		switch (getItemViewType(position)) {
			case EXAMPLE_VIEW_TYPE:
				ExampleViewHolder sHolder = (ExampleViewHolder) holder;
				sHolder.mImageView.setImageResource(R.drawable.ic_account_circle_white_24dp);
				sHolder.itemView.setActivated(true);
				sHolder.mTitle.setSelected(true);//For marquee
				sHolder.mTitle.setText(Html.fromHtml(item.getTitle()));
				sHolder.mSubtitle.setText(Html.fromHtml(item.getSubtitle()));
				animateView(holder.itemView, position, false);
				return;

			default:
				ViewHolder vHolder = (ViewHolder) holder;
				//When user scrolls, this line binds the correct selection status
				vHolder.itemView.setActivated(isSelected(position));

				//ANIMATION EXAMPLE!! ImageView - Handle Flip Animation on Select and Deselect ALL
				if (mSelectAll || mLastItemInActionMode) {
					//Reset the flags with delay
					vHolder.mImageView.postDelayed(new Runnable() {
						@Override
						public void run() {
							mSelectAll = mLastItemInActionMode = false;
						}
					}, 200L);
					//Consume the Animation
					//flip(holder.mImageView, isSelected(position), 200L);
				} else {
					//Display the current flip status
					//setFlipped(holder.mImageView, isSelected(position));
				}

				//This "if-else" is just an example
				if (isSelected(position)) {
					vHolder.mImageView.setBackgroundDrawable(mContext.getResources().getDrawable(R.drawable.image_round_selected));
					animateView(holder.itemView, position, true);
				} else {
					vHolder.mImageView.setBackgroundDrawable(mContext.getResources().getDrawable(R.drawable.image_round_normal));
					animateView(holder.itemView, position, false);
				}

				vHolder.mImageView.setImageResource(R.drawable.ic_account_circle_white_24dp);

				//In case of searchText matches with Title or with an Item's field
				// this will be highlighted
				if (hasSearchText()) {
					setHighlightText(vHolder.mTitle, item.getTitle(), mSearchText);
					setHighlightText(vHolder.mSubtitle, item.getSubtitle(), mSearchText);
				} else {
					vHolder.mTitle.setText(item.getTitle());
					vHolder.mSubtitle.setText(item.getSubtitle());
				}
		}//end-switch
	}

	@Override
	public List<Animator> getAnimators(View itemView, int position, boolean isSelected) {
		List<Animator> animators = new ArrayList<Animator>();
		//Alpha Animator is needed (it will be added automatically if not here)
		addAlphaAnimator(animators, itemView, 0);

		//LinearLayout
		switch (getItemViewType(position)) {
			case EXAMPLE_VIEW_TYPE:
				addScaleInAnimator(animators, itemView, 0.0f);
				break;
			default:
				if (isSelected)
					addSlideInFromRightAnimator(animators, itemView, 0.5f);
				else
					addSlideInFromLeftAnimator(animators, itemView, 0.5f);
				break;
		}

		//GridLayout
//		if (position % 2 != 0)
//			addSlideInFromRightAnimator(animators, view, 0.5f);
//		else
//			addSlideInFromLeftAnimator(animators, view, 0.5f);

		return animators;
	}

	@Override
	public String getTextToShowInBubble(int position) {
		if (!DatabaseService.userLearnedSelection && position == 0) {//This 'if' is for my example only
			//This is the normal line you should use: Usually it's the first letter
			return getItem(position).getTitle().substring(0, 1).toUpperCase();
		}
		return getItem(position).getTitle().substring(5); //This is for my example only
	}

	private void setHighlightText(TextView textView, String text, String searchText) {
		Spannable spanText = Spannable.Factory.getInstance().newSpannable(text);
		int i = text.toLowerCase(Locale.getDefault()).indexOf(searchText);
		if (i != -1) {
			spanText.setSpan(new ForegroundColorSpan(Utils.getColorAccent(mContext)), i,
					i + searchText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			spanText.setSpan(new StyleSpan(Typeface.BOLD), i,
					i + searchText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
			textView.setText(spanText, TextView.BufferType.SPANNABLE);
		} else {
			textView.setText(text, TextView.BufferType.NORMAL);
		}
	}

	/**
	 * Custom filter.
	 *
	 * @param myObject   The item to filter
	 * @param constraint the current searchText
	 * @return true if a match exists in the title or in the subtitle, false if no match found.
	 */
	@Override
	protected boolean filterObject(Item myObject, String constraint) {
		String valueText = myObject.getTitle();
		//Filter on Title
		if (valueText != null && valueText.toLowerCase().contains(constraint)) {
			return true;
		}
		//Filter on Subtitle
		valueText = myObject.getSubtitle();
		return valueText != null && valueText.toLowerCase().contains(constraint);
	}

	/**
	 * Used for UserLearnsSelection.
	 */
	static class ExampleViewHolder extends FlexibleViewHolder {

		ImageView mImageView;
		TextView mTitle;
		TextView mSubtitle;
		ImageView mDismissIcon;

		ExampleViewHolder(View view, final ExampleAdapter adapter) {
			super(view, adapter, null);
			mTitle = (TextView) view.findViewById(R.id.title);
			mSubtitle = (TextView) view.findViewById(R.id.subtitle);
			mImageView = (ImageView) view.findViewById(R.id.image);
			mDismissIcon = (ImageView) view.findViewById(R.id.dismiss_icon);
			mDismissIcon.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					adapter.userLearnedSelection();
				}
			});
		}
	}

	/**
	 * Provide a reference to the views for each data item.
	 * Complex data labels may need more than one view per item, and
	 * you provide access to all the views for a data item in a view holder.
	 */
	static final class ViewHolder extends FlexibleViewHolder {
		ImageView mImageView;
		TextView mTitle;
		TextView mSubtitle;
		Context mContext;

		ViewHolder(View view, final ExampleAdapter adapter) {
			super(view, adapter, adapter.mClickListener);
			this.mContext = adapter.mContext;

			this.mTitle = (TextView) view.findViewById(R.id.title);
			this.mSubtitle = (TextView) view.findViewById(R.id.subtitle);
			this.mImageView = (ImageView) view.findViewById(R.id.image);
			this.mImageView.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					mListItemClickListener.onListItemLongClick(getAdapterPosition());
					toggleActivation();
				}
			});
		}

		@Override
		protected void toggleActivation() {
			super.toggleActivation();
			//This "if-else" is just an example
			if (itemView.isActivated()) {
				mImageView.setBackgroundDrawable(mContext.getResources().getDrawable(R.drawable.image_round_selected));
			} else {
				mImageView.setBackgroundDrawable(mContext.getResources().getDrawable(R.drawable.image_round_normal));
			}
			//Example of custom Animation inside the ItemView
			//flip(mImageView, itemView.isActivated());
		}
	}

	@Override
	public String toString() {
		return mItems.toString();
	}

}