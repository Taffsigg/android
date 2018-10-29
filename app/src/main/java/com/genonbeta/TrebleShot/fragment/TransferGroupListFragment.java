package com.genonbeta.TrebleShot.fragment;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.genonbeta.TrebleShot.R;
import com.genonbeta.TrebleShot.activity.ViewTransferActivity;
import com.genonbeta.TrebleShot.adapter.TransferGroupListAdapter;
import com.genonbeta.TrebleShot.app.EditableListFragment;
import com.genonbeta.TrebleShot.app.EditableListFragmentImpl;
import com.genonbeta.TrebleShot.app.GroupEditableListFragment;
import com.genonbeta.TrebleShot.database.AccessDatabase;
import com.genonbeta.TrebleShot.service.CommunicationService;
import com.genonbeta.TrebleShot.ui.callback.IconSupport;
import com.genonbeta.TrebleShot.ui.callback.TitleSupport;
import com.genonbeta.TrebleShot.util.AppUtils;
import com.genonbeta.TrebleShot.widget.GroupEditableListAdapter;
import com.genonbeta.android.database.SQLQuery;
import com.genonbeta.android.framework.widget.PowerfulActionMode;

import java.util.ArrayList;
import java.util.Map;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * created by: Veli
 * date: 10.11.2017 00:15
 */

public class TransferGroupListFragment
		extends GroupEditableListFragment<TransferGroupListAdapter.PreloadedGroup, GroupEditableListAdapter.GroupViewHolder, TransferGroupListAdapter>
		implements IconSupport, TitleSupport
{
	public SQLQuery.Select mSelect;
	public IntentFilter mFilter = new IntentFilter();
	public BroadcastReceiver mReceiver = new BroadcastReceiver()
	{
		@Override
		public void onReceive(Context context, Intent intent)
		{
			if (AccessDatabase.ACTION_DATABASE_CHANGE.equals(intent.getAction())
					&& intent.hasExtra(AccessDatabase.EXTRA_TABLE_NAME)
					&& (intent.getStringExtra(AccessDatabase.EXTRA_TABLE_NAME).equals(AccessDatabase.TABLE_TRANSFERGROUP)
					|| intent.getStringExtra(AccessDatabase.EXTRA_TABLE_NAME).equals(AccessDatabase.TABLE_TRANSFER)
			))
				refreshList();
			else if (CommunicationService.ACTION_TASK_RUNNING_LIST_CHANGE.equals(intent.getAction())
					&& intent.hasExtra(CommunicationService.EXTRA_TASK_LIST_RUNNING)) {
				getAdapter().updateActiveList(intent.getLongArrayExtra(CommunicationService.EXTRA_TASK_LIST_RUNNING));
				refreshList();
			}
		}
	};

	@Override
	public void onCreate(@Nullable Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setDefaultOrderingCriteria(TransferGroupListAdapter.MODE_SORT_ORDER_DESCENDING);
		setDefaultSortingCriteria(TransferGroupListAdapter.MODE_SORT_BY_DATE);
		setDefaultGroupingCriteria(TransferGroupListAdapter.MODE_GROUP_BY_DATE);
		setDefaultSelectionCallback(new SelectionCallback(this));
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
	{
		super.onViewCreated(view, savedInstanceState);

		setEmptyImage(R.drawable.ic_compare_arrows_white_24dp);
		setEmptyText(getString(R.string.text_listEmptyTransfer));
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState)
	{
		super.onActivityCreated(savedInstanceState);

		mFilter.addAction(AccessDatabase.ACTION_DATABASE_CHANGE);
		mFilter.addAction(CommunicationService.ACTION_TASK_RUNNING_LIST_CHANGE);

		if (getSelect() == null)
			setSelect(new SQLQuery.Select(AccessDatabase.TABLE_TRANSFERGROUP));
	}

	@Override
	public void onResume()
	{
		super.onResume();
		getActivity().registerReceiver(mReceiver, mFilter);

		AppUtils.startForegroundService(getActivity(), new Intent(getActivity(), CommunicationService.class)
				.setAction(CommunicationService.ACTION_REQUEST_TASK_RUNNING_LIST_CHANGE));
	}

	@Override
	public void onPause()
	{
		super.onPause();
		getActivity().unregisterReceiver(mReceiver);
	}

	@Override
	public void onSortingOptions(Map<String, Integer> options)
	{
		options.put(getString(R.string.text_sortByDate), TransferGroupListAdapter.MODE_SORT_BY_DATE);
		options.put(getString(R.string.text_sortBySize), TransferGroupListAdapter.MODE_SORT_BY_SIZE);
	}

	@Override
	public void onGroupingOptions(Map<String, Integer> options)
	{
		options.put(getString(R.string.text_groupByNothing), TransferGroupListAdapter.MODE_GROUP_BY_NOTHING);
		options.put(getString(R.string.text_groupByDate), TransferGroupListAdapter.MODE_GROUP_BY_DATE);
	}

	@Override
	public int onGridSpanSize(int viewType, int currentSpanSize)
	{
		return viewType == TransferGroupListAdapter.VIEW_TYPE_REPRESENTATIVE
				? currentSpanSize
				: super.onGridSpanSize(viewType, currentSpanSize);
	}

	@Override
	public TransferGroupListAdapter onAdapter()
	{
		final AppUtils.QuickActions<GroupEditableListAdapter.GroupViewHolder> quickActions = new AppUtils.QuickActions<GroupEditableListAdapter.GroupViewHolder>()
		{
			@Override
			public void onQuickActions(final GroupEditableListAdapter.GroupViewHolder clazz)
			{
				if (!clazz.isRepresentative()) {
					registerLayoutViewClicks(clazz);

					if (getSelectionConnection() != null)
						clazz.getView().findViewById(R.id.layout_image).setOnClickListener(new View.OnClickListener()
						{
							@Override
							public void onClick(View v)
							{
								getSelectionConnection().setSelected(clazz.getAdapterPosition());
							}
						});
				}
			}
		};

		return new TransferGroupListAdapter(getActivity(), AppUtils.getDatabase(getContext()))
		{
			@NonNull
			@Override
			public GroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType)
			{
				return AppUtils.quickAction(super.onCreateViewHolder(parent, viewType), quickActions);
			}
		}.setSelect(getSelect());
	}

	@Override
	public boolean onDefaultClickAction(GroupEditableListAdapter.GroupViewHolder holder)
	{
		try {
			ViewTransferActivity.startInstance(getActivity(), getAdapter().getItem(holder).groupId);
			return true;
		} catch (Exception e) {
		}

		return false;
	}

	@Override
	public int getIconRes()
	{
		return R.drawable.ic_swap_vert_white_24dp;
	}

	@Override
	public CharSequence getTitle(Context context)
	{
		return context.getString(R.string.text_transfers);
	}

	public SQLQuery.Select getSelect()
	{
		return mSelect;
	}

	public TransferGroupListFragment setSelect(SQLQuery.Select select)
	{
		mSelect = select;
		return this;
	}

	private static class SelectionCallback extends EditableListFragment.SelectionCallback<TransferGroupListAdapter.PreloadedGroup>
	{
		public SelectionCallback(EditableListFragmentImpl<TransferGroupListAdapter.PreloadedGroup> fragment)
		{
			super(fragment);
		}

		@Override
		public boolean onPrepareActionMenu(Context context, PowerfulActionMode actionMode)
		{
			super.onPrepareActionMenu(context, actionMode);
			return true;
		}

		@Override
		public boolean onCreateActionMenu(Context context, PowerfulActionMode actionMode, Menu menu)
		{
			super.onCreateActionMenu(context, actionMode, menu);
			actionMode.getMenuInflater().inflate(R.menu.action_mode_group, menu);
			return true;
		}

		@Override
		public boolean onActionMenuItemSelected(Context context, PowerfulActionMode actionMode, MenuItem item)
		{
			int id = item.getItemId();

			ArrayList<TransferGroupListAdapter.PreloadedGroup> selectionList = getFragment().getSelectionConnection().getSelectedItemList();

			if (id == R.id.action_mode_group_delete) {
				for (TransferGroupListAdapter.PreloadedGroup preloadedGroup : selectionList)
					AppUtils.getDatabase(getFragment().getContext()).remove(preloadedGroup);
			} else
				return super.onActionMenuItemSelected(context, actionMode, item);

			return true;
		}
	}
}